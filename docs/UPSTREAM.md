# Upstream Repositories and Baselines

## Termux App
- **Repository:** `https://github.com/termux/termux-app`
- **Pinned Tag / Release:** `v0.118.1`
- **Package Transformation:** Replaced `com.termux` with `com.agentdesk.app`
- **Target SDK:** 28 (permits binary execution from private data directories on Android 10-15)
- **ABI Filter:** `arm64-v8a` only
- **Bootstrap Architecture:** aarch64 minimal (bash, coreutils, proot, tar, xz, ca-certificates)

## Ubuntu Rootfs (proot-distro)
- **Distro:** Ubuntu 22.04 LTS (Jammy Jellyfish) aarch64
- **Rootfs Guest Home:** `/root`
- **Projects Guest Path:** `/root/projects`
