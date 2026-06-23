#!/bin/sh
# deploy.sh — Deploy Commit Storyteller to Railway
# 1. Crea cuenta en https://railway.app
# 2. railway login
# 3. railway up

set -e

PATH="$HOME/.railway/bin:$PATH"

echo "=== Commit Storyteller — Deploy ==="

# Check Railway CLI
if ! command -v railway >/dev/null 2>&1; then
  echo "Installing Railway CLI..."
  sh -c "$(curl -sL https://railway.app/install.sh)"
fi

echo "Railway CLI: $(railway --version 2>/dev/null || echo 'ok')"

# Check if logged in
if ! railway whoami >/dev/null 2>&1; then
  echo ""
  echo "=> Run 'railway login' to authenticate"
  echo "   (opens browser for GitHub OAuth)"
  echo ""
  echo "Then run: railway init && railway up"
  exit 0
fi

echo "Logged in as: $(railway whoami 2>/dev/null)"

# Init project if needed
if [ ! -f railway.json ]; then
  echo "Initializing Railway project..."
  railway init
fi

echo "Deploying..."
railway up --detach

echo ""
echo "✅ Deployed! Run 'railway domain' to get your URL"
