Slice-Wallet for Android
----------------------------------

![download](/images/icon-google-play.png)(#)

### Non-custodial, secure Slicecoin wallet

Slice-Wallet is the best way to get started with . Our simple, streamlined design is easy for beginners, yet powerful enough for experienced users. This is a port of the SliceWallet for iOS app which can be found here: https://github.com/Slicecoin-foundation/Slicewallet-ios

##### Completely decentralized

Unlike other iOS Slicecoin wallets, **SliceWallet** is a standalone Slicecoin client. It connects directly to the Slicecoin network using [SPV](https://en.bitcoin.it/wiki/Thin_Client_Security#Header-Only_Clients) mode, and doesn't rely on servers that can be hacked or disabled. Even if SliceWallet is removed from the App Store, the app will continue to function, allowing users to access their money at any time.

##### Cutting-edge security

**Slice-Wallet** utilizes AES hardware encryption, app sandboxing, and the latest iOS security features to protect users from malware, browser security holes, and even physical theft. Private keys are stored only in the secure enclave of the user's phone, inaccessible to anyone other than the user.

##### Desgined with new users in mind

Simplicity and ease-of-use is **Slice-Wallet**'s core design principle. A simple recovery phrase (which we call a paper key) is all that is needed to restore the user's wallet if they ever lose or replace their device. **SliceWallet** is [deterministic](https://github.com/bitcoin/bips/blob/master/bip-0032.mediawiki), which means the user's balance and transaction history can be recovered just from the paper key.

###### Features:

- ["simplified payment verification"](https://github.com/bitcoin/bips/blob/master/bip-0037.mediawiki) for fast mobile performance
- no server to get hacked or go down
- single backup phrase that works forever
- private keys never leave your device
- import [password protected](https://github.com/bitcoin/bips/blob/master/bip-0038.mediawiki) paper wallets
- ["payment protocol"](https://github.com/bitcoin/bips/blob/master/bip-0070.mediawiki) payee identity certification

### Building & Developing SliceWallet for Android:

1. Download and install Java 7 or up
2. Download and install the latest Android studio
3. Download NDK r15c from the [NDK Archives](https://developer.android.com/ndk/downloads/older_releases.html)
4. Clone this repo & init submodules
```bash
$ git clone https://github.com/Slicecoin-foundation/Slicewallet-android
$ git submodule init
$ git submodule update
```
5. Open the project with Android Studio, navigate to `File > Project Structure > SDK Location`
6. Change `Android NDK Location` with the path to NDK r15c that you downloaded earlier
7. Go to SDK Manager and download all the SDK Platforms and SDK Tools
9. Build -> Rebuild Project
