# Puzzlr Share Website

Hosted on GitHub Pages. Firebase Realtime Database backend.

---

## Deploy to GitHub Pages (5 min)

1. Create a new GitHub repo called `puzzlr-share` (public)
2. Upload `index.html` to the repo root
3. Go to **Settings → Pages → Source → main branch / root**
4. Your site will be live at: `https://YOUR_USERNAME.github.io/puzzlr-share`

---

## Set Firebase Database Rules

1. Go to [Firebase Console](https://console.firebase.google.com)
2. Open project **david-connection**
3. Go to **Realtime Database → Rules**
4. Paste the contents of `firebase-rules.json` and click **Publish**

---

## Update the App Install Link

In `index.html`, find this line and replace with your real Play Store URL:

```js
const APP_INSTALL_BASE = "https://play.google.com/store/apps/details?id=com.puzzlr.app";
```

---

## Update SITE_URL (if needed)

The site auto-detects its own URL. No change needed unless you use a custom domain.

---

## How the App reads rewards (Option B — auto-detect)

In your Puzzlr HTML, add this code. It silently checks Firebase when online and
unlocks the reward automatically:

```js
// ── AUTO REWARD CHECK (paste inside Puzzlr app HTML) ──────────────
const FIREBASE_DB = "https://david-connection-default-rtdb.firebaseio.com";

async function checkShareReward() {
  const code = localStorage.getItem('puzzlr_share_code');
  if (!code || !navigator.onLine) return;
  try {
    const res = await fetch(`${FIREBASE_DB}/users/${code}.json`);
    const user = await res.json();
    if (!user || !user.rewardReady) return;

    const benefit  = user.rewardBenefit;
    const rwdCode  = user.rewardCode;

    // Apply the benefit
    if (benefit === 'passes' || benefit === 'passes10') {
      const add = benefit === 'passes' ? 5 : 10;
      const cur = parseInt(localStorage.getItem('offlinePasses') || '0');
      localStorage.setItem('offlinePasses', String(cur + add));
      toast(`🎟️ +${add} Offline Passes unlocked!`, 'gold', 5000);
    } else if (benefit === 'adfree') {
      localStorage.setItem('adFreeUntil', String(Date.now() + 24*60*60*1000));
      toast('🚫 Ad-Free for 24 hours unlocked!', 'gold', 5000);
    } else if (benefit === 'unlock') {
      localStorage.setItem('themeUnlocked', 'true');
      toast('🔓 New theme unlocked!', 'gold', 5000);
    }

    // Clear reward from Firebase so it doesn't fire again
    await fetch(`${FIREBASE_DB}/users/${code}/rewardReady.json`, {
      method: 'PUT', body: 'false',
      headers: { 'Content-Type': 'application/json' }
    });
    await fetch(`${FIREBASE_DB}/users/${code}/rewardBenefit.json`, {
      method: 'PUT', body: 'null',
      headers: { 'Content-Type': 'application/json' }
    });

    updateOfflinePassBanner(); // refresh pass display in UI
  } catch(e) {
    console.log('[Reward check]', e.message);
  }
}

// Run on app startup and every time app comes online
checkShareReward();
window.addEventListener('online', checkShareReward);
```

Also store the user's share code when they register on the website:
The website uses `localStorage.setItem('puzzlr_share_code', code)` —
since both the website and app run in the same WebView, they share the
same localStorage. No extra code needed for that part.

---

## Manual reward code (Option A)

Add a text field somewhere in the Puzzlr app settings:

```js
async function redeemRewardCode(inputCode) {
  const FIREBASE_DB = "https://david-connection-default-rtdb.firebaseio.com";
  // Find which user owns this reward code
  // (You'd need to query by rewardCode — set up a Firebase index for this)
  // For now, user enters their share code + reward code together
}
```

Option B (auto-detect) is recommended — no manual entry needed.

---

## Chain reaction summary

```
User A → gets PZLR-XXXX → shares link
User B → opens link → confirms PZLR-XXXX on site → User A gets reward in app
User B → gets PZLR-YYYY → shares link
User C → opens link → confirms PZLR-YYYY on site → User B gets reward in app
... and so on forever
```
