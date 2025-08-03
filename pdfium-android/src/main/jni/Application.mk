APP_STL := c++_shared
APP_CPPFLAGS += -fexceptions
APP_SUPPORT_FLEXIBLE_PAGE_SIZES := true

#For ANativeWindow support
APP_PLATFORM = android-23

APP_ABI :=  armeabi-v7a \
            arm64-v8a \
            mips \
            mips64 \
            x86 \
            x86_64