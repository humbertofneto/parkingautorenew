# Diagnostic Logging Guide

## Overview
Comprehensive logging has been added to MainActivity.kt to debug two issues:
1. **WebView not capturing dynamic pages** - Always returns page 1 data
2. **Keyboard not opening** - IME picker appears or nothing happens

## Log Points Added

### 1. **onCreate()** 
- Logs when activity is created
- Tracks EditText focus listener setup
- No specific log yet, but check for errors during initialization

### 2. **GET INFO Button Click**
```
Log: "GET INFO clicked - URL: <url>"
Log: "New URL detected. Loading: <url>" (if new URL)
Log: "loadUrl() called for: <url>" (if new URL)
Log: "Delay 2000ms completed, calling extractPageInfo()" (after 2s)
Log: "Same URL. Capturing current DOM state..." (if same URL)
Log: "Delay 500ms completed, calling extractPageInfo()" (after 0.5s)
```

### 3. **initializeWebView()**
```
Log: "=== initializeWebView() START ==="
Log: "WebView instance created"
Log: "WebView settings applied: JS enabled=true, DOM Storage enabled=true"
Log: "PageBridge interface added to WebView"
Log: "onPageStarted: <url>" (when page starts loading)
Log: "onPageFinished: <url> - Ready to extract data" (when page is fully loaded)
Log: "WebView error - URL: <url>, Error code: <code>, Description: <description>" (if error occurs)
Log: "=== initializeWebView() COMPLETE ==="
```

### 4. **extractPageInfo()**
```
Log: "=== extractPageInfo() START - Page attempt #<N> ==="
Log: "isWebViewInitialized=true, webView existence=true"
Log: "Calling webView.evaluateJavascript()..."
Log: "evaluateJavascript callback - Result: <result>"
Log: "=== extractPageInfo() END ==="
```

### 5. **showKeyboard()**
```
Log: "showKeyboard() called"
Log: "urlInput focused: <bool>, hasFocus: <bool>"
Log: "InputMethodManager obtained"
Log: "showSoftInput result: <bool> (true = shown, false = hidden)"
Log: "Alternative showSoftInput called with SHOW_FORCED"
```

### 6. **PageBridge.onPageInfo() - JavaScript Callback**
```
Log: "=== onPageInfo RECEIVED ===" (SUCCESS - JavaScript called the method!)
Log: "JSON length: <N> chars"
Log: "First 200 chars: <json_start>"
Log: "Full JSON: <complete_json>"
Log: "captureCount incremented to: <N>"
Log: "UI updated with captured page #<N>"
```

### 7. **PageBridge.onError() - JavaScript Error**
```
Log: "=== onError RECEIVED ===" (Means JavaScript found an error)
Log: "Error message: <error_text>"
Log: "UI updated with error message"
```

## How to Capture Logs on Windows Device

### Step 1: Build and Install
```bash
# On your macOS machine
cd /Users/humferre/localcode/autorenew
./gradlew assembleDebug

# Then use adb to install (ensure Windows device is connected via USB with Developer Mode ON)
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Step 2: Clear and Start Logcat
```bash
# Clear previous logs
adb logcat --clear

# Start capturing logs in real-time
adb logcat -v threadtime "MainActivity*:D" "PageBridge*:D" "WebView*:D" "*:I"

# OR: Capture to file
adb logcat > ~/Desktop/app_logs.txt
```

### Step 3: Reproduce the Issue
1. Open the app on Windows device
2. Click in URL field (observe keyboard behavior - log shows what InputMethodManager reports)
3. Enter: `https://www.offstreet.io/location/LWLN9BUO`
4. Click **GET INFO** (observe logs for URL loading)
5. Wait 2 seconds (logs show when page finishes loading)
6. Observe if page data appears or error
7. Navigate to different page in the SPA
8. Click **GET INFO** again (logs show if JavaScript executes)

### Step 4: Analyze Logs

**Expected Success Sequence:**
```
D/MainActivity: GET INFO clicked - URL: https://www.offstreet.io/location/LWLN9BUO
D/MainActivity: New URL detected. Loading: https://www.offstreet.io/location/LWLN9BUO
D/MainActivity: loadUrl() called for: https://www.offstreet.io/location/LWLN9BUO
D/MainActivity: onPageStarted: https://www.offstreet.io/location/LWLN9BUO
D/MainActivity: onPageFinished: https://www.offstreet.io/location/LWLN9BUO - Ready to extract data
D/MainActivity: Delay 2000ms completed, calling extractPageInfo()
D/MainActivity: extractPageInfo() START - Page attempt #1
D/MainActivity: Calling webView.evaluateJavascript()...
D/MainActivity: evaluateJavascript callback - Result: ...
D/PageBridge: === onPageInfo RECEIVED ===
D/PageBridge: JSON length: XXXX chars
D/PageBridge: Full JSON: {...page: 1, inputs: [...], buttons: [...]}
```

**If JavaScript Doesn't Execute:**
```
D/MainActivity: GET INFO clicked - URL: https://www.offstreet.io/location/LWLN9BUO
D/MainActivity: Same URL. Capturing current DOM state...
D/MainActivity: Delay 500ms completed, calling extractPageInfo()
D/MainActivity: extractPageInfo() START - Page attempt #2
D/MainActivity: Calling webView.evaluateJavascript()...
D/MainActivity: evaluateJavascript callback - Result: null
(NO PageBridge log appears)
```
↑ This indicates JavaScript didn't execute

**If WebView Never Loads:**
```
D/MainActivity: GET INFO clicked - URL: https://www.offstreet.io/location/LWLN9BUO
D/MainActivity: New URL detected. Loading: https://www.offstreet.io/location/LWLN9BUO
D/MainActivity: loadUrl() called for: https://www.offstreet.io/location/LWLN9BUO
(No onPageStarted or onPageFinished logs appear)
```
↑ This indicates WebView isn't loading the URL

**If Keyboard Problem:**
```
D/MainActivity: showKeyboard() called
D/MainActivity: urlInput focused: false, hasFocus: false
D/MainActivity: InputMethodManager obtained
D/MainActivity: showSoftInput result: false
D/MainActivity: Alternative showSoftInput called with SHOW_FORCED
```
↑ The `false` result means keyboard won't show. Focus state is important!

## Key Things to Check

1. **WebView Lifecycle**
   - Search logs for "onPageFinished" 
   - If absent: WebView not loading URLs properly
   - If present: Page loaded, JavaScript should work

2. **JavaScript Execution**
   - Search logs for "onPageInfo RECEIVED"
   - If present: JavaScript bridge works, data captured
   - If absent + "onPageFinished present": WebView exists but JS not executing

3. **Keyboard**
   - Search logs for "showSoftInput result"
   - If `true`: Keyboard should appear (check visually)
   - If `false`: Keyboard won't show (focus/permission issue)

4. **Error Details**
   - Search logs for "Error message:" or "WebView error"
   - These contain specific failure reasons

## Common Issues & Solutions

| Issue | Log Signature | Likely Cause |
|-------|---------------|--------------|
| Page 2 still shows Page 1 data | onPageInfo shows `"page": 1` twice | JavaScript sees old DOM, or WebView not synchronized |
| No JSON appears at all | No "onPageInfo RECEIVED" | JavaScript not executing or bridge misconfigured |
| Keyboard appears then disappears | showSoftInput result: true | Manifest settings may override programmatic calls |
| App crashes | Exception stack trace | Check if WebView destroyed before access |

## Push to GitHub
This version has been pushed to `feature/url-input-mapper` branch.

To build on Windows:
```bash
./gradlew assembleDebug
# Install via adb to your device
adb install -r app/build/outputs/apk/debug/app-debug.apk
```
