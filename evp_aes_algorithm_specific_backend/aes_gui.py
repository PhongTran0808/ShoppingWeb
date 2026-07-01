"""
PyQt6 GUI for evpaesdirect.dll

This GUI uses the algorithm-specific OpenSSL EVP AES DLL exported by:

    include/evp_aes_direct_c_api.h
    src/evp_aes_direct_c_api.cpp

DLL name expected:
    evpaesdirect.dll

Supported direct EVP AES modes depend on the C++ backend, typically:

Classical/general:
    aes-128/192/256-ecb, cbc, ctr, ofb, cfb, cfb8, cfb1
    aes-128/192/256-wrap, wrap-pad
    aes-128-xts, aes-256-xts

AEAD:
    aes-128/192/256-gcm
    aes-128/192/256-ccm
    aes-128/192/256-ocb

Not supported by this direct EVP-accessor backend:
    AES-EAX, AES-SIV, AES-GCM-SIV

Install:
    pip install PyQt6

Run:
    python evpaesdirect_pyqt6_gui.py

Place evpaesdirect.dll beside this .py file, or select it in the GUI.
"""

from __future__ import annotations

import ctypes
import re
import sys
from pathlib import Path
from typing import Optional, Tuple

from PyQt6.QtWidgets import (
    QApplication,
    QComboBox,
    QFileDialog,
    QGridLayout,
    QGroupBox,
    QHBoxLayout,
    QLabel,
    QLineEdit,
    QMainWindow,
    QMessageBox,
    QPushButton,
    QPlainTextEdit,
    QSpinBox,
    QTabWidget,
    QVBoxLayout,
    QWidget,
)


EVPAESDIRECT_OK = 0
EVPAESDIRECT_ERR_BUFFER_TOO_SMALL = -2
EVPAESDIRECT_ERR_AUTH_FAILED = -3


def clean_hex(s: str) -> str:
    """
    Accept teaching-friendly hex forms:
        001122
        00 11 22
        00:11:22
        0x00 0x11 0x22
    """
    s = s.strip()
    s = re.sub(r"0x", "", s, flags=re.IGNORECASE)
    s = re.sub(r"[^0-9a-fA-F]", "", s)
    return s


def hex_to_bytes_py(s: str, field_name: str = "hex") -> bytes:
    s = clean_hex(s)

    if not s:
        return b""

    if len(s) % 2 != 0:
        raise ValueError(f"{field_name} must contain an even number of hex digits")

    try:
        return bytes.fromhex(s)
    except ValueError as exc:
        raise ValueError(f"Invalid {field_name}") from exc


def bytes_to_text_lossy(data: bytes) -> str:
    return data.decode("utf-8", errors="replace")


def as_u8_array(data: bytes):
    if not data:
        return None
    return (ctypes.c_uint8 * len(data)).from_buffer_copy(data)


def as_u8_ptr(arr):
    if arr is None:
        return None
    return ctypes.cast(arr, ctypes.POINTER(ctypes.c_uint8))


def is_aead_name(cipher_name: str) -> bool:
    c = cipher_name.lower()
    return "-gcm" in c or "-ccm" in c or "-ocb" in c


def is_ivless_classical(cipher_name: str) -> bool:
    c = cipher_name.lower()
    return "-ecb" in c or "-wrap" in c


def classical_uses_iv(cipher_name: str) -> bool:
    return (not is_aead_name(cipher_name)) and (not is_ivless_classical(cipher_name))


def default_padding_for_cipher(cipher_name: str) -> bool:
    c = cipher_name.lower()
    return "-ecb" in c or "-cbc" in c


class EVPAESDirectDLL:
    """
    ctypes wrapper for evpaesdirect.dll.

    Uses the two-pass buffer-sizing pattern:
        1. call with output buffer = NULL and capacity 0
        2. DLL returns required output length with ERR_BUFFER_TOO_SMALL
        3. allocate buffer
        4. call again
    """

    def __init__(self, dll_path: str):
        self.dll_path = str(Path(dll_path).resolve())
        self.lib = ctypes.CDLL(self.dll_path, mode=ctypes.RTLD_LOCAL)
        self._bind()

    def _bind(self) -> None:
        u8p = ctypes.POINTER(ctypes.c_uint8)
        sizep = ctypes.POINTER(ctypes.c_size_t)
        intp = ctypes.POINTER(ctypes.c_int)

        self.lib.evpaesdirect_version.argtypes = []
        self.lib.evpaesdirect_version.restype = ctypes.c_char_p

        self.lib.evpaesdirect_last_error.argtypes = []
        self.lib.evpaesdirect_last_error.restype = ctypes.c_char_p

        self.lib.evpaesdirect_clear_error.argtypes = []
        self.lib.evpaesdirect_clear_error.restype = None

        self.lib.evpaesdirect_cipher_available.argtypes = [ctypes.c_char_p]
        self.lib.evpaesdirect_cipher_available.restype = ctypes.c_int

        self.lib.evpaesdirect_cipher_expected_key_len.argtypes = [ctypes.c_char_p, sizep]
        self.lib.evpaesdirect_cipher_expected_key_len.restype = ctypes.c_int

        self.lib.evpaesdirect_cipher_expected_iv_len.argtypes = [ctypes.c_char_p, sizep]
        self.lib.evpaesdirect_cipher_expected_iv_len.restype = ctypes.c_int

        self.lib.evpaesdirect_cipher_block_size.argtypes = [ctypes.c_char_p, intp]
        self.lib.evpaesdirect_cipher_block_size.restype = ctypes.c_int

        self.lib.evpaesdirect_encrypt.argtypes = [
            ctypes.c_char_p,
            u8p, ctypes.c_size_t,
            u8p, ctypes.c_size_t,
            u8p, ctypes.c_size_t,
            ctypes.c_int,
            u8p, ctypes.c_size_t,
            sizep,
        ]
        self.lib.evpaesdirect_encrypt.restype = ctypes.c_int

        self.lib.evpaesdirect_decrypt.argtypes = [
            ctypes.c_char_p,
            u8p, ctypes.c_size_t,
            u8p, ctypes.c_size_t,
            u8p, ctypes.c_size_t,
            ctypes.c_int,
            u8p, ctypes.c_size_t,
            sizep,
        ]
        self.lib.evpaesdirect_decrypt.restype = ctypes.c_int

        self.lib.evpaesdirect_aead_encrypt.argtypes = [
            ctypes.c_char_p,
            u8p, ctypes.c_size_t,
            u8p, ctypes.c_size_t,
            u8p, ctypes.c_size_t,
            u8p, ctypes.c_size_t,
            ctypes.c_size_t,
            u8p, ctypes.c_size_t,
            sizep,
            u8p, ctypes.c_size_t,
            sizep,
        ]
        self.lib.evpaesdirect_aead_encrypt.restype = ctypes.c_int

        self.lib.evpaesdirect_aead_decrypt.argtypes = [
            ctypes.c_char_p,
            u8p, ctypes.c_size_t,
            u8p, ctypes.c_size_t,
            u8p, ctypes.c_size_t,
            u8p, ctypes.c_size_t,
            u8p, ctypes.c_size_t,
            u8p, ctypes.c_size_t,
            sizep,
        ]
        self.lib.evpaesdirect_aead_decrypt.restype = ctypes.c_int

    def version(self) -> str:
        raw = self.lib.evpaesdirect_version()
        return raw.decode("utf-8", errors="replace") if raw else "unknown"

    def last_error(self) -> str:
        raw = self.lib.evpaesdirect_last_error()
        return raw.decode("utf-8", errors="replace") if raw else ""

    def _check(self, status: int, allowed: Tuple[int, ...] = (EVPAESDIRECT_OK,)) -> None:
        if status not in allowed:
            msg = self.last_error() or f"EVPAESDirect error status {status}"
            raise RuntimeError(msg)

    def cipher_available(self, cipher_name: str) -> bool:
        status = self.lib.evpaesdirect_cipher_available(cipher_name.encode("utf-8"))
        if status in (0, 1):
            return bool(status)
        self._check(status)
        return False

    def expected_key_len(self, cipher_name: str) -> int:
        out = ctypes.c_size_t(0)
        status = self.lib.evpaesdirect_cipher_expected_key_len(cipher_name.encode("utf-8"), ctypes.byref(out))
        self._check(status)
        return int(out.value)

    def expected_iv_len(self, cipher_name: str) -> int:
        out = ctypes.c_size_t(0)
        status = self.lib.evpaesdirect_cipher_expected_iv_len(cipher_name.encode("utf-8"), ctypes.byref(out))
        self._check(status)
        return int(out.value)

    def block_size(self, cipher_name: str) -> int:
        out = ctypes.c_int(0)
        status = self.lib.evpaesdirect_cipher_block_size(cipher_name.encode("utf-8"), ctypes.byref(out))
        self._check(status)
        return int(out.value)

    def encrypt(self, cipher_name: str, key: bytes, iv: bytes, plaintext: bytes, padding: bool) -> bytes:
        cipher_b = cipher_name.encode("utf-8")
        key_arr = as_u8_array(key)
        iv_arr = as_u8_array(iv)
        pt_arr = as_u8_array(plaintext)
        out_len = ctypes.c_size_t(0)

        status = self.lib.evpaesdirect_encrypt(
            cipher_b,
            as_u8_ptr(key_arr), len(key),
            as_u8_ptr(iv_arr), len(iv),
            as_u8_ptr(pt_arr), len(plaintext),
            1 if padding else 0,
            None, 0,
            ctypes.byref(out_len),
        )
        self._check(status, allowed=(EVPAESDIRECT_OK, EVPAESDIRECT_ERR_BUFFER_TOO_SMALL))

        if out_len.value == 0:
            return b""

        out_buf = (ctypes.c_uint8 * out_len.value)()
        status = self.lib.evpaesdirect_encrypt(
            cipher_b,
            as_u8_ptr(key_arr), len(key),
            as_u8_ptr(iv_arr), len(iv),
            as_u8_ptr(pt_arr), len(plaintext),
            1 if padding else 0,
            out_buf, out_len.value,
            ctypes.byref(out_len),
        )
        self._check(status)
        return bytes(out_buf[: out_len.value])

    def decrypt(self, cipher_name: str, key: bytes, iv: bytes, ciphertext: bytes, padding: bool) -> bytes:
        cipher_b = cipher_name.encode("utf-8")
        key_arr = as_u8_array(key)
        iv_arr = as_u8_array(iv)
        ct_arr = as_u8_array(ciphertext)
        out_len = ctypes.c_size_t(0)

        status = self.lib.evpaesdirect_decrypt(
            cipher_b,
            as_u8_ptr(key_arr), len(key),
            as_u8_ptr(iv_arr), len(iv),
            as_u8_ptr(ct_arr), len(ciphertext),
            1 if padding else 0,
            None, 0,
            ctypes.byref(out_len),
        )
        self._check(status, allowed=(EVPAESDIRECT_OK, EVPAESDIRECT_ERR_BUFFER_TOO_SMALL))

        if out_len.value == 0:
            return b""

        out_buf = (ctypes.c_uint8 * out_len.value)()
        status = self.lib.evpaesdirect_decrypt(
            cipher_b,
            as_u8_ptr(key_arr), len(key),
            as_u8_ptr(iv_arr), len(iv),
            as_u8_ptr(ct_arr), len(ciphertext),
            1 if padding else 0,
            out_buf, out_len.value,
            ctypes.byref(out_len),
        )
        self._check(status)
        return bytes(out_buf[: out_len.value])

    def aead_encrypt(self, cipher_name: str, key: bytes, nonce: bytes, aad: bytes, plaintext: bytes, tag_len: int) -> Tuple[bytes, bytes]:
        cipher_b = cipher_name.encode("utf-8")
        key_arr = as_u8_array(key)
        nonce_arr = as_u8_array(nonce)
        aad_arr = as_u8_array(aad)
        pt_arr = as_u8_array(plaintext)
        ct_len = ctypes.c_size_t(0)
        tag_out_len = ctypes.c_size_t(0)

        status = self.lib.evpaesdirect_aead_encrypt(
            cipher_b,
            as_u8_ptr(key_arr), len(key),
            as_u8_ptr(nonce_arr), len(nonce),
            as_u8_ptr(aad_arr), len(aad),
            as_u8_ptr(pt_arr), len(plaintext),
            int(tag_len),
            None, 0, ctypes.byref(ct_len),
            None, 0, ctypes.byref(tag_out_len),
        )
        self._check(status, allowed=(EVPAESDIRECT_OK, EVPAESDIRECT_ERR_BUFFER_TOO_SMALL))

        ct_buf = (ctypes.c_uint8 * ct_len.value)() if ct_len.value else None
        tag_buf = (ctypes.c_uint8 * tag_out_len.value)() if tag_out_len.value else None

        status = self.lib.evpaesdirect_aead_encrypt(
            cipher_b,
            as_u8_ptr(key_arr), len(key),
            as_u8_ptr(nonce_arr), len(nonce),
            as_u8_ptr(aad_arr), len(aad),
            as_u8_ptr(pt_arr), len(plaintext),
            int(tag_len),
            ct_buf, ct_len.value, ctypes.byref(ct_len),
            tag_buf, tag_out_len.value, ctypes.byref(tag_out_len),
        )
        self._check(status)

        ciphertext = bytes(ct_buf[: ct_len.value]) if ct_buf is not None else b""
        tag = bytes(tag_buf[: tag_out_len.value]) if tag_buf is not None else b""
        return ciphertext, tag

    def aead_decrypt(self, cipher_name: str, key: bytes, nonce: bytes, aad: bytes, ciphertext: bytes, tag: bytes) -> bytes:
        cipher_b = cipher_name.encode("utf-8")
        key_arr = as_u8_array(key)
        nonce_arr = as_u8_array(nonce)
        aad_arr = as_u8_array(aad)
        ct_arr = as_u8_array(ciphertext)
        tag_arr = as_u8_array(tag)
        out_len = ctypes.c_size_t(0)

        status = self.lib.evpaesdirect_aead_decrypt(
            cipher_b,
            as_u8_ptr(key_arr), len(key),
            as_u8_ptr(nonce_arr), len(nonce),
            as_u8_ptr(aad_arr), len(aad),
            as_u8_ptr(ct_arr), len(ciphertext),
            as_u8_ptr(tag_arr), len(tag),
            None, 0,
            ctypes.byref(out_len),
        )
        if status == EVPAESDIRECT_ERR_AUTH_FAILED:
            raise RuntimeError("AEAD authentication failed: tag/AAD/nonce/ciphertext mismatch")
        self._check(status, allowed=(EVPAESDIRECT_OK, EVPAESDIRECT_ERR_BUFFER_TOO_SMALL))

        if out_len.value == 0:
            return b""

        out_buf = (ctypes.c_uint8 * out_len.value)()
        status = self.lib.evpaesdirect_aead_decrypt(
            cipher_b,
            as_u8_ptr(key_arr), len(key),
            as_u8_ptr(nonce_arr), len(nonce),
            as_u8_ptr(aad_arr), len(aad),
            as_u8_ptr(ct_arr), len(ciphertext),
            as_u8_ptr(tag_arr), len(tag),
            out_buf, out_len.value,
            ctypes.byref(out_len),
        )
        if status == EVPAESDIRECT_ERR_AUTH_FAILED:
            raise RuntimeError("AEAD authentication failed: tag/AAD/nonce/ciphertext mismatch")
        self._check(status)
        return bytes(out_buf[: out_len.value])


class MainWindow(QMainWindow):
    CLASSICAL_MODES = [
        "aes-128-ecb", "aes-192-ecb", "aes-256-ecb",
        "aes-128-cbc", "aes-192-cbc", "aes-256-cbc",
        "aes-128-ctr", "aes-192-ctr", "aes-256-ctr",
        "aes-128-ofb", "aes-192-ofb", "aes-256-ofb",
        "aes-128-cfb", "aes-192-cfb", "aes-256-cfb",
        "aes-128-cfb8", "aes-192-cfb8", "aes-256-cfb8",
        "aes-128-cfb1", "aes-192-cfb1", "aes-256-cfb1",
        "aes-128-wrap", "aes-192-wrap", "aes-256-wrap",
        "aes-128-wrap-pad", "aes-192-wrap-pad", "aes-256-wrap-pad",
        "aes-128-xts", "aes-256-xts",
    ]

    AEAD_MODES = [
        "aes-128-gcm", "aes-192-gcm", "aes-256-gcm",
        "aes-128-ccm", "aes-192-ccm", "aes-256-ccm",
        "aes-128-ocb", "aes-192-ocb", "aes-256-ocb",
    ]

    def __init__(self):
        super().__init__()
        self.dll: Optional[EVPAESDirectDLL] = None
        self.setWindowTitle("EVP AES Direct GUI - PyQt6 + evpaesdirect.dll")
        self.resize(1120, 820)
        self._build_ui()
        self._auto_find_dll()
        self._update_fields()

    def _build_ui(self) -> None:
        root = QWidget()
        main_layout = QVBoxLayout(root)

        dll_box = QGroupBox("DLL")
        dll_layout = QGridLayout(dll_box)

        self.dll_path = QLineEdit()
        self.dll_path.setPlaceholderText("Path to evpaesdirect.dll")
        self.btn_browse_dll = QPushButton("Browse...")
        self.btn_load_dll = QPushButton("Load DLL")
        self.lbl_dll_status = QLabel("Not loaded")
        self.btn_browse_dll.clicked.connect(self._browse_dll)
        self.btn_load_dll.clicked.connect(self._load_dll)

        dll_layout.addWidget(QLabel("DLL path:"), 0, 0)
        dll_layout.addWidget(self.dll_path, 0, 1)
        dll_layout.addWidget(self.btn_browse_dll, 0, 2)
        dll_layout.addWidget(self.btn_load_dll, 0, 3)
        dll_layout.addWidget(self.lbl_dll_status, 1, 1, 1, 3)
        main_layout.addWidget(dll_box)

        config_box = QGroupBox("Configuration")
        config_layout = QGridLayout(config_box)

        self.operation = QComboBox()
        self.operation.addItems(["Encrypt", "Decrypt"])

        self.cipher = QComboBox()
        self.cipher.addItems(self.CLASSICAL_MODES + self.AEAD_MODES)

        self.padding = QComboBox()
        self.padding.addItems(["auto", "padding", "nopad"])

        self.key_hex = QPlainTextEdit()
        self.key_hex.setMaximumHeight(72)
        self.key_hex.setPlaceholderText(
            "Key in hex. For XTS, key length is doubled: AES-128-XTS uses 32 bytes; AES-256-XTS uses 64 bytes."
        )

        self.iv_hex = QLineEdit()
        self.iv_hex.setPlaceholderText("IV/tweak in hex for CBC/CTR/OFB/CFB/XTS")

        self.nonce_hex = QLineEdit()
        self.nonce_hex.setPlaceholderText("AEAD nonce/IV in hex")

        self.tag_len = QSpinBox()
        self.tag_len.setRange(1, 16)
        self.tag_len.setValue(16)

        self.aad_type = QComboBox()
        self.aad_type.addItems(["Hex", "Text"])

        self.aad = QPlainTextEdit()
        self.aad.setMaximumHeight(72)
        self.aad.setPlaceholderText("AAD for AEAD modes")

        self.tag_hex = QPlainTextEdit()
        self.tag_hex.setMaximumHeight(72)
        self.tag_hex.setPlaceholderText("Tag hex for AEAD decryption; generated tag for AEAD encryption")

        self.cipher.currentTextChanged.connect(self._update_fields)
        self.operation.currentTextChanged.connect(self._update_fields)

        config_layout.addWidget(QLabel("Operation:"), 0, 0)
        config_layout.addWidget(self.operation, 0, 1)
        config_layout.addWidget(QLabel("Cipher:"), 0, 2)
        config_layout.addWidget(self.cipher, 0, 3)
        config_layout.addWidget(QLabel("Padding:"), 0, 4)
        config_layout.addWidget(self.padding, 0, 5)

        self.btn_random_key = QPushButton("Random Key & IV")
        self.btn_random_key.clicked.connect(self._generate_random_key)

        config_layout.addWidget(QLabel("Key hex:"), 1, 0)
        config_layout.addWidget(self.key_hex, 1, 1, 1, 4)
        config_layout.addWidget(self.btn_random_key, 1, 5)
        config_layout.addWidget(QLabel("IV/tweak hex:"), 2, 0)
        config_layout.addWidget(self.iv_hex, 2, 1, 1, 5)
        config_layout.addWidget(QLabel("Nonce hex:"), 3, 0)
        config_layout.addWidget(self.nonce_hex, 3, 1, 1, 3)
        config_layout.addWidget(QLabel("Tag length:"), 3, 4)
        config_layout.addWidget(self.tag_len, 3, 5)
        config_layout.addWidget(QLabel("AAD type:"), 4, 0)
        config_layout.addWidget(self.aad_type, 4, 1)
        config_layout.addWidget(QLabel("AAD:"), 5, 0)
        config_layout.addWidget(self.aad, 5, 1, 1, 5)
        config_layout.addWidget(QLabel("Tag hex:"), 6, 0)
        config_layout.addWidget(self.tag_hex, 6, 1, 1, 5)
        main_layout.addWidget(config_box)

        tabs = QTabWidget()
        input_tab = QWidget()
        input_layout = QVBoxLayout(input_tab)
        input_top = QHBoxLayout()

        self.input_type = QComboBox()
        self.input_type.addItems(["Text", "Hex", "File"])
        self.input_file = QLineEdit()
        self.input_file.setPlaceholderText("Input file path")
        self.btn_input_file = QPushButton("Browse Input...")
        self.btn_input_file.clicked.connect(self._browse_input_file)

        input_top.addWidget(QLabel("Input type:"))
        input_top.addWidget(self.input_type)
        input_top.addWidget(self.input_file, 1)
        input_top.addWidget(self.btn_input_file)

        self.input_data = QPlainTextEdit()
        self.input_data.setPlaceholderText("Input text or hex")
        input_layout.addLayout(input_top)
        input_layout.addWidget(self.input_data)

        output_tab = QWidget()
        output_layout = QVBoxLayout(output_tab)
        output_top = QHBoxLayout()

        self.output_type = QComboBox()
        self.output_type.addItems(["Hex", "Text", "File"])
        self.output_file = QLineEdit()
        self.output_file.setPlaceholderText("Output file path")
        self.btn_output_file = QPushButton("Browse Output...")
        self.btn_output_file.clicked.connect(self._browse_output_file)

        output_top.addWidget(QLabel("Output type:"))
        output_top.addWidget(self.output_type)
        output_top.addWidget(self.output_file, 1)
        output_top.addWidget(self.btn_output_file)

        self.output_data = QPlainTextEdit()
        self.output_data.setPlaceholderText("Output")
        output_layout.addLayout(output_top)
        output_layout.addWidget(self.output_data)

        tabs.addTab(input_tab, "Input")
        tabs.addTab(output_tab, "Output")
        main_layout.addWidget(tabs, 1)

        button_row = QHBoxLayout()
        self.btn_execute = QPushButton("Execute")
        self.btn_clear = QPushButton("Clear Output")
        self.btn_gcm_vector = QPushButton("Load AES-GCM Vector")
        self.btn_info = QPushButton("Cipher Info")
        self.btn_check = QPushButton("Check Availability")

        self.btn_execute.clicked.connect(self._execute)
        self.btn_clear.clicked.connect(self.output_data.clear)
        self.btn_gcm_vector.clicked.connect(self._load_gcm_vector)
        self.btn_info.clicked.connect(self._show_cipher_info)
        self.btn_check.clicked.connect(self._check_availability)

        button_row.addWidget(self.btn_execute)
        button_row.addWidget(self.btn_clear)
        button_row.addWidget(self.btn_gcm_vector)
        button_row.addWidget(self.btn_info)
        button_row.addWidget(self.btn_check)
        main_layout.addLayout(button_row)

        self.setCentralWidget(root)

    def _auto_find_dll(self) -> None:
        candidates = [Path.cwd() / "evpaesdirect.dll", Path(__file__).resolve().parent / "evpaesdirect.dll"]
        for p in candidates:
            if p.exists():
                self.dll_path.setText(str(p))
                return

    def _browse_dll(self) -> None:
        path, _ = QFileDialog.getOpenFileName(self, "Select evpaesdirect.dll", "", "DLL Files (*.dll);;All Files (*)")
        if path:
            self.dll_path.setText(path)

    def _browse_input_file(self) -> None:
        path, _ = QFileDialog.getOpenFileName(self, "Select input file", "", "All Files (*)")
        if path:
            self.input_file.setText(path)
            self.input_type.setCurrentText("File")

    def _browse_output_file(self) -> None:
        path, _ = QFileDialog.getSaveFileName(self, "Select output file", "", "All Files (*)")
        if path:
            self.output_file.setText(path)
            self.output_type.setCurrentText("File")

    def _load_dll(self) -> None:
        path = self.dll_path.text().strip()
        if not path:
            self._error("Please select evpaesdirect.dll")
            return
        try:
            self.dll = EVPAESDirectDLL(path)
            self.lbl_dll_status.setText(f"Loaded: {self.dll.version()}")
            self.lbl_dll_status.setStyleSheet("color: green;")
            self.statusBar().showMessage("DLL loaded", 5000)
        except Exception as exc:
            self.dll = None
            self.lbl_dll_status.setText("Load failed")
            self.lbl_dll_status.setStyleSheet("color: red;")
            self._error(str(exc))

    def _require_dll(self) -> EVPAESDirectDLL:
        if self.dll is None:
            self._load_dll()
        if self.dll is None:
            raise RuntimeError("evpaesdirect.dll is not loaded")
        return self.dll

    def _update_fields(self) -> None:
        cipher = self.cipher.currentText()
        op = self.operation.currentText()
        aead = is_aead_name(cipher)

        self.nonce_hex.setEnabled(aead)
        self.aad_type.setEnabled(aead)
        self.aad.setEnabled(aead)
        self.tag_len.setEnabled(aead and op == "Encrypt")
        self.tag_hex.setEnabled(aead)
        self.iv_hex.setEnabled(classical_uses_iv(cipher))
        self.padding.setEnabled(not aead)

        if aead:
            self.padding.setCurrentText("nopad")
        elif not default_padding_for_cipher(cipher):
            self.padding.setCurrentText("nopad")
        else:
            self.padding.setCurrentText("auto")

    def _read_input_bytes(self) -> bytes:
        input_type = self.input_type.currentText()
        if input_type == "File":
            path = self.input_file.text().strip()
            if not path:
                raise ValueError("Input file path is empty")
            return Path(path).read_bytes()
        text = self.input_data.toPlainText()
        if input_type == "Hex":
            return hex_to_bytes_py(text, "input hex")
        return text.encode("utf-8")

    def _read_aad_bytes(self) -> bytes:
        if not is_aead_name(self.cipher.currentText()):
            return b""
        if self.aad_type.currentText() == "Hex":
            return hex_to_bytes_py(self.aad.toPlainText(), "AAD hex")
        return self.aad.toPlainText().encode("utf-8")

    def _write_output_bytes(self, data: bytes) -> None:
        output_type = self.output_type.currentText()
        if output_type == "File":
            path = self.output_file.text().strip()
            if not path:
                raise ValueError("Output file path is empty")
            Path(path).write_bytes(data)
            self.output_data.setPlainText(f"Wrote {len(data)} byte(s) to:\n{path}")
            return
        if output_type == "Text":
            self.output_data.setPlainText(bytes_to_text_lossy(data))
            return
        self.output_data.setPlainText(data.hex())

    def _selected_padding_bool(self, cipher: str) -> bool:
        p = self.padding.currentText()
        if p == "padding":
            return True
        if p == "nopad":
            return False
        return default_padding_for_cipher(cipher)

    def _execute(self) -> None:
        try:
            dll = self._require_dll()
            cipher = self.cipher.currentText()
            operation = self.operation.currentText()
            key = hex_to_bytes_py(self.key_hex.toPlainText(), "key hex")
            data = self._read_input_bytes()

            if is_aead_name(cipher):
                nonce = hex_to_bytes_py(self.nonce_hex.text(), "nonce hex")
                aad = self._read_aad_bytes()
                if operation == "Encrypt":
                    ciphertext, tag = dll.aead_encrypt(cipher, key, nonce, aad, data, int(self.tag_len.value()))
                    self._write_output_bytes(ciphertext)
                    self.tag_hex.setPlainText(tag.hex())
                else:
                    tag = hex_to_bytes_py(self.tag_hex.toPlainText(), "tag hex")
                    plaintext = dll.aead_decrypt(cipher, key, nonce, aad, data, tag)
                    self._write_output_bytes(plaintext)
            else:
                iv = b""
                if classical_uses_iv(cipher):
                    iv = hex_to_bytes_py(self.iv_hex.text(), "IV/tweak hex")
                padding = self._selected_padding_bool(cipher)
                if operation == "Encrypt":
                    out = dll.encrypt(cipher, key, iv, data, padding)
                else:
                    out = dll.decrypt(cipher, key, iv, data, padding)
                self._write_output_bytes(out)

            self.statusBar().showMessage("Operation completed", 5000)
        except Exception as exc:
            self._error(str(exc))

    def _load_gcm_vector(self) -> None:
        self.operation.setCurrentText("Encrypt")
        self.cipher.setCurrentText("aes-128-gcm")
        self.key_hex.setPlainText("00000000000000000000000000000000")
        self.nonce_hex.setText("000000000000000000000000")
        self.aad_type.setCurrentText("Hex")
        self.aad.setPlainText("")
        self.input_type.setCurrentText("Hex")
        self.input_data.setPlainText("00000000000000000000000000000000")
        self.output_type.setCurrentText("Hex")
        self.tag_len.setValue(16)
        self.tag_hex.clear()
        self.output_data.clear()
        self._update_fields()
        self.statusBar().showMessage(
            "Expected ciphertext: 0388dace60b6a392f328c2b971b2fe78; tag: ab6e47d42cec13bdf53a67b21257bddf",
            12000,
        )

    def _show_cipher_info(self) -> None:
        try:
            dll = self._require_dll()
            cipher = self.cipher.currentText()
            key_len = dll.expected_key_len(cipher)
            iv_len = dll.expected_iv_len(cipher)
            block_size = dll.block_size(cipher)
            available = dll.cipher_available(cipher)
            QMessageBox.information(
                self,
                "Cipher Info",
                f"Cipher: {cipher}\n"
                f"Available: {'yes' if available else 'no'}\n"
                f"Expected key length: {key_len} byte(s), {key_len * 8} bits\n"
                f"Expected IV length: {iv_len} byte(s)\n"
                f"Block size: {block_size} byte(s)\n\n"
                f"Key hex length should be {key_len * 2} hex characters.",
            )
        except Exception as exc:
            self._error(str(exc))

    def _check_availability(self) -> None:
        try:
            dll = self._require_dll()
            cipher = self.cipher.currentText()
            available = dll.cipher_available(cipher)
            QMessageBox.information(self, "Availability", f"{cipher}: {'available' if available else 'not available'}")
        except Exception as exc:
            self._error(str(exc))

    def _error(self, msg: str) -> None:
        QMessageBox.critical(self, "EVP AES Direct Error", msg)
        self.statusBar().showMessage("Error", 5000)

    def _generate_random_key(self) -> None:
        try:
            dll = self._require_dll()
            cipher = self.cipher.currentText()
            
            key_len = dll.expected_key_len(cipher)
            iv_len = dll.expected_iv_len(cipher)
            
            import os
            random_key = os.urandom(key_len)
            self.key_hex.setPlainText(random_key.hex())
            
            if iv_len > 0:
                random_iv = os.urandom(iv_len)
                if is_aead_name(cipher):
                    self.nonce_hex.setText(random_iv.hex())
                else:
                    self.iv_hex.setText(random_iv.hex())
                    
        except Exception as exc:
            self._error(f"Cannot generate key/IV (DLL loaded?): {exc}")



def main() -> int:
    app = QApplication(sys.argv)
    win = MainWindow()
    win.show()
    return app.exec()


if __name__ == "__main__":
    raise SystemExit(main())
