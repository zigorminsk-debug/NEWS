# Правила R8/ProGuard для release-сборки TechPulse.

# OkHttp: необязательные TLS-провайдеры отсутствуют на Android — это нормально.
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# OkHttp озабочен отсутствием классов на некоторых платформах
-keepattributes Signature, InnerClasses, EnclosingMethod, RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations, AnnotationDefault

-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn org.slf4j.**
