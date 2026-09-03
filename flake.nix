{
  description = "KMP Compose Multiplatform development environment";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs =
    {
      self,
      nixpkgs,
      flake-utils,
    }:
    flake-utils.lib.eachDefaultSystem (
      system:
      let
        pkgs = import nixpkgs {
          inherit system;
          config = {
            allowUnfree = true;
            android_sdk.accept_license = true;
          };
        };

        androidComposition = pkgs.androidenv.composeAndroidPackages {
          platformVersions = [
            "34"
            "36"
          ];
          buildToolsVersions = [
            "34.0.0"
            "36.0.0"
          ];
          platformToolsVersion = "37.0.1";
          cmdLineToolsVersion = "11.0";
          includeEmulator = false;
          includeSystemImages = false;
        };

        androidSdk = androidComposition.androidsdk;
      in
      {
        devShells.default = pkgs.mkShell {
          buildInputs = [
            pkgs.jdk21
            pkgs.gradle
            pkgs.android-studio
            androidSdk
          ];

          ANDROID_HOME = "${androidSdk}/libexec/android-sdk";
          ANDROID_SDK_ROOT = "${androidSdk}/libexec/android-sdk";
          JAVA_HOME = pkgs.jdk21.home;

          LD_LIBRARY_PATH = pkgs.lib.makeLibraryPath [
            pkgs.libGL
            pkgs.fontconfig
            pkgs.libx11
            pkgs.libxcursor
            pkgs.libxext
            pkgs.libxrandr
            pkgs.libxinerama
          ];

          shellHook = ''
            echo "Android SDK: $ANDROID_HOME"
            echo "Java: $JAVA_HOME"
            export GRADLE_OPTS="-Dorg.gradle.project.android.aapt2FromMavenOverride=$ANDROID_SDK_ROOT/build-tools/36.0.0/aapt2"
            export _JAVA_AWT_WM_NONREPARENTING=1
          '';
        };
      }
    );
}
