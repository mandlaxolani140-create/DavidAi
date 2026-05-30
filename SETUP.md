# Puzzlr Share Site — Setup Guide
## Two things to set up: GitHub Pages (website) + Firebase (database rules)
---

## STEP 1 — PUT THE SITE ON GITHUB PAGES

1. Go to https://github.com and sign in (or create a free account)

2. Click the green **"New"** button to create a new repository
   - Name it: `puzzlr-share`
   - Set it to **Public**
   - Click **"Create repository"**

3. Upload the file:
   - Click **"uploading an existing file"**
   - Drag and drop `index.html` from this folder
   - Click **"Commit changes"**

4. Enable GitHub Pages:
   - Go to your repo **Settings** → scroll to **"Pages"** (left sidebar)
   - Under "Source" select **"Deploy from a branch"**
   - Branch: **main** / Folder: **/ (root)**
   - Click **Save**

5. Wait ~60 seconds. Your site will be live at:
   `https://YOUR-GITHUB-USERNAME.github.io/puzzlr-share`

6. **Update the SITE_URL in index.html:**
   - Open `index.html`
   - Find this line near the top of the `<script>`:
     ```
     const SITE_URL = window.location.origin + ...
     ```
   - This is automatic — no change needed ✅

7. **Update the APP_INSTALL_BASE link:**
   - Find this line:
     ```
     const APP_INSTALL_BASE = "https://play.google.com/store/apps/details?id=com.puzzlr.app";
     ```
   - Replace `com.puzzlr.app` with your real Play Store package name

---

## STEP 2 — SET FIREBASE DATABASE RULES

Your database is currently open (test mode). You must set the security rules
so that users can only write their own data and can't overwrite others.

1. Go to https://console.firebase.google.com
2. Open your project: **david-connection**
3. In the left sidebar click **"Realtime Database"**
4. Click the **"Rules"** tab at the top
5. Delete everything in the rules editor
6. Copy and paste the contents of `firebase-rules.json` (from this folder)
7. Click **"Publish"**

That's it. The rules mean:
- Anyone can read user profiles and referrals (needed for the site to work)
- A user code can only be written once (prevents overwriting)
- A referral confirmation can only be written once (prevents duplicate rewards)

---

## STEP 3 — ADD AUTO-UNLOCK TO THE PUZZLR APP (HTML)

The app needs to check Firebase on startup and unlock rewards automatically.
Add this script block to your `puzzlr_hotspot_fixed.html` just before `</body>`:

```html
<script type="module">
import { initializeApp } from "https://www.gstatic.com/firebasejs/12.14.0/firebase-app.js";
import { getDatabase, ref, get, update } from "https://www.gstatic.com/firebasejs/12.14.0/firebase-database.js";

const firebaseConfig = {
  apiKey: "AIzaSyBFBJSyWKd_GGoQ5IhUZQ9w8j4-HaOeD1s",
  authDomain: "david-connection.firebaseapp.com",
  databaseURL: "https://david-connection-default-rtdb.firebaseio.com",
  projectId: "david-connection",
  storageBucket: "david-connection.firebasestorage.app",
  messagingSenderId: "214764400544",
  appId: "1:214764400544:web:0d2d9b3ab52f44e3033ec5"
};

const fbApp = initializeApp(firebaseConfig, "puzzlr-rewards");
const db    = getDatabase(fbApp);

// Run check when app loads (only if online)
window.addEventListener('load', () => {
  if (!navigator.onLine) return;
  checkPuzzlrReward();
});

window.checkPuzzlrReward = async function() {
  // Get saved share code from localStorage (set when user registers on share site)
  const code = localStorage.getItem('puzzlr_share_code');
  if (!code) return;

  try {
    const snap = await get(ref(db, 'users/' + code));
    if (!snap.exists()) return;
    const u = snap.val();

    // Only fire if reward is ready and not yet applied in app
    if (!u.rewardReady) return;
    const alreadyApplied = localStorage.getItem('puzzlr_reward_applied_' + u.rewardCode);
    if (alreadyApplied) return;

    // Apply the benefit
    applyRewardBenefit(u.rewardBenefit, u.rewardCode);

    // Mark as applied locally
    localStorage.setItem('puzzlr_reward_applied_' + u.rewardCode, '1');

    // Clear rewardReady on Firebase so it doesn't fire again
    await update(ref(db, 'users/' + code), { rewardReady: false, currentRewardCode: null });

  } catch(e) {
    console.warn('[Puzzlr Reward] Check failed:', e.message);
  }
};

function applyRewardBenefit(benefit, code) {
  // ── Add passes ──
  if (benefit === 'passes' || benefit === 'passes10') {
    const amount = benefit === 'passes10' ? 10 : 5;
    const key    = 'offlinePasses_v2';
    const now    = Date.now();
    let stored;
    try { stored = JSON.parse(localStorage.getItem(key)); } catch(e) { stored = null; }
    let current = 0;
    if (stored && stored.resetAt > now) current = stored.count || 0;
    const newCount = current + amount;
    localStorage.setItem(key, JSON.stringify({ count: newCount, resetAt: stored?.resetAt || (now + 86400000) }));
    showRewardToast('🎟️ +' + amount + ' Offline Passes unlocked!', 'green');
    return;
  }

  // ── Ad-free 24h ──
  if (benefit === 'adfree') {
    localStorage.setItem('adFreeUntil', Date.now() + 24 * 60 * 60 * 1000);
    if (window._updateAdFreeCard) window._updateAdFreeCard();
    showRewardToast('🚫 Ad-Free for 24 hours unlocked!', 'gold');
    return;
  }

  // ── Theme unlock ──
  if (benefit === 'unlock') {
    const themes = JSON.parse(localStorage.getItem('puzzlr_unlocked_themes') || '[]');
    if (!themes.includes('referral_theme')) {
      themes.push('referral_theme');
      localStorage.setItem('puzzlr_unlocked_themes', JSON.stringify(themes));
    }
    showRewardToast('🔓 New theme unlocked!', 'gold');
    return;
  }
}

function showRewardToast(msg, type) {
  // Use the app's existing toast if available
  if (window.toast) { window.toast(msg, type, 6000); return; }
  // Fallback
  const d = document.createElement('div');
  d.textContent = msg;
  d.style.cssText = 'position:fixed;bottom:30px;left:50%;transform:translateX(-50%);background:#1a1a2e;color:#fff;padding:12px 22px;border-radius:12px;font-size:0.8rem;font-weight:700;z-index:99999;border:1px solid rgba(0,255,136,0.4);';
  document.body.appendChild(d);
  setTimeout(() => d.remove(), 5000);
}

// Also expose manual redeem via reward code (Option A backup)
window.redeemRewardCode = async function(inputCode) {
  if (!inputCode) return false;
  const code = localStorage.getItem('puzzlr_share_code');
  if (!code) return false;
  try {
    const snap = await get(ref(db, 'users/' + code));
    if (!snap.exists()) return false;
    const u = snap.val();
    if (u.rewardCode !== inputCode.trim().toUpperCase()) return false;
    if (localStorage.getItem('puzzlr_reward_applied_' + u.rewardCode)) {
      if (window.toast) window.toast('This reward code has already been used.', 'red');
      return false;
    }
    applyRewardBenefit(u.rewardBenefit, u.rewardCode);
    localStorage.setItem('puzzlr_reward_applied_' + u.rewardCode, '1');
    await update(ref(db, 'users/' + code), { rewardReady: false, currentRewardCode: null });
    return true;
  } catch(e) {
    console.warn('[Puzzlr Reward] Redeem failed:', e.message);
    return false;
  }
};
</script>
```

---

## STEP 4 — LINK THE SHARE SITE FROM INSIDE THE APP (optional but recommended)

Add a "Share & Earn" button somewhere in your app's main menu that opens:
```
https://YOUR-GITHUB-USERNAME.github.io/puzzlr-share
```

When the user visits, they register and their share code gets saved to
`localStorage` in the WebView — so the auto-unlock check in Step 3 can
find it next time the app opens.

---

## HOW THE FULL CHAIN WORKS (summary)

```
User A                    Share Site                 Firebase DB           User B
  │                           │                           │                   │
  ├─ visits site ────────────►│                           │                   │
  ├─ registers ──────────────►│─── creates code ─────────►│                  │
  │◄── gets PZLR-XXXX ────────┤◄── stores user ───────────┤                  │
  │                           │                           │                   │
  ├─ shares install link + code ──────────────────────────────────────────────►│
  │                           │                           │              installs app
  │                           │                           │◄── User B visits site
  │                           │                           │    enters User A's code
  │◄── pendingRewards +1 ─────────────────────────────────┤                   │
  │                           │                           │                   │
  ├─ opens Puzzlr app ───────────────────────────────────►│                   │
  │◄── auto-unlock fires ─────────────────────────────────┤                   │
  │    (passes / ad-free / theme)                         │                   │
```

---

## FILES IN THIS FOLDER

| File | Purpose |
|---|---|
| `index.html` | The full share website — upload this to GitHub |
| `firebase-rules.json` | Paste these into Firebase Console → Realtime DB → Rules |
| `SETUP.md` | This guide |
