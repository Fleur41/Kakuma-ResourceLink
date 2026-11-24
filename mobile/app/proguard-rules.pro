# Moshi uses KSP code generation only (no reflection); moshi, moshi-kotlin-codegen, Retrofit,
# OkHttp, Room and Hilt ship their own consumer R8 rules, so no extra keep rules are needed here.
# A minified, release-signed build was run on an emulator against the local API (Nov 2026):
# sign-in, home with opportunities and the stories feed parsed correctly, no R8 errors in logcat.

# Keep line numbers in stack traces for crash reports; the mapping file de-obfuscates names.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
