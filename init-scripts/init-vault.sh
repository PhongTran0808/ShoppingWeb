#!/bin/sh

export VAULT_ADDR='http://127.0.0.1:8200'
export VAULT_TOKEN='root'

echo "Waiting for Vault to start..."
sleep 5

echo "Enabling Transit Secrets Engine..."
vault secrets enable transit

echo "Creating encryption key for e-commerce orders..."
vault write -f transit/keys/ecommerce-order-key

echo "Enabling KV Secrets Engine v2..."
vault secrets enable -path=secret kv-v2

echo "Storing HMAC shared secret for service-to-service authentication..."
vault kv put secret/ecommerce hmac_secret="super_secret_hmac_key_for_ecommerce_platform_2026"

echo "Vault initialization completed!"
