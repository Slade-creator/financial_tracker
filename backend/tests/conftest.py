"""Shared test configuration.

- Puts the backend root on sys.path so `import main`, `from Model...`,
  `from services...` work the same way they do when uvicorn runs from backend/.
- Provides a dummy OpenRouter API key before any module imports config.Settings
  (which requires the key to be present).
"""

import os
import sys
from pathlib import Path

BACKEND_ROOT = Path(__file__).resolve().parent.parent
if str(BACKEND_ROOT) not in sys.path:
    sys.path.insert(0, str(BACKEND_ROOT))

os.environ.setdefault("OPENROUTER_API_KEY", "test-key-not-real")
