# FocusBlock - app + website blocker (Kotlin, native Android)

## APK kaise banaye

**Option A: Android Studio (sabse easy)**
1. Android Studio install karo, `File > Open` se is folder ko open karo.
2. Gradle sync hone do (pehli baar 5-10 min lagte hain).
3. `Build > Build Bundle(s) / APK(s) > Build APK(s)`.
4. APK yahan milega: `app/build/outputs/apk/debug/app-debug.apk`

**Option B: GitHub Actions (laptop pe kuch install nahi karna)**
1. Is folder ko ek GitHub repo me push karo.
2. Repo ke `Actions` tab me "Build APK" run hone do.
3. Run ke end me `FocusBlock-apk` artifact download karo.

## Phone me chalane ke steps
1. APK install karo (Unknown sources allow karna padega).
2. App kholo, `Open Accessibility Settings` dabao, FocusBlock ko ON karo.
3. **Android 13+ pe agar toggle grey hai:** Settings > Apps > FocusBlock > top-right `⋮` >
   `Allow restricted settings`, phir dubara accessibility me jao.
4. App me Apps / Websites tab se jo block karna hai select karo.

## Kaise kaam karta hai
- `BlockerService` ek AccessibilityService hai. Jab koi blocked app khulta hai to HOME press
  karwata hai aur 2.5 sec ka "blocked" overlay dikhata hai.
- Websites ke liye browser ka address bar text padhta hai (Chrome, Brave, Edge, Firefox,
  Samsung Internet, Opera, DuckDuckGo) aur domain/subdomain match hone par block karta hai.
- Settings app list me nahi dikhti taaki tum khud ko lock na kar lo.

## Limitations
- Sirf upar wale browsers me website blocking chalti hai; baaki browsers ke ids add karne padenge
  (`BlockerService.kt` ka `urlBars` map).
- Koi PIN nahi hai: koi bhi accessibility se service band kar sakta hai.
- Kisi ne code compile nahi kiya, pehli build me chhoti errors aa sakti hain.
