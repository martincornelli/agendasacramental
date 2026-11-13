# Add project specific ProGuard rules here.
# Room 2.6 creates WorkManager's database through Class.newInstance().
# AGP 9 no longer implicitly keeps default constructors for class-only rules.
-keepclassmembers class androidx.work.impl.WorkDatabase_Impl {
    public <init>();
}
# Firebase also discovers its component registrars through no-argument constructors.
-keepclassmembers class * implements com.google.firebase.components.ComponentRegistrar {
    public <init>();
}
# One-time work instantiates its input merger by name as well.
-keepclassmembers class * extends androidx.work.InputMerger {
    public <init>();
}

# Firestore reads these models (including nested objects) through reflection.
# Preserve property names and no-argument constructors to retain the stored schema.
-keep class com.example.agendasacramental.Unidad { *; }
-keep class com.example.agendasacramental.Agenda { *; }
-keep class com.example.agendasacramental.AceptacionAsignacion { *; }
-keep class com.example.agendasacramental.AsuntoEstacaBarrio { *; }
-keep class com.example.agendasacramental.MensajeEvangelio { *; }
-keep class com.example.agendasacramental.Hermano { *; }
-keep class com.example.agendasacramental.ConfiguracionPlanificacion { *; }

# Enum names are persisted in Firestore and must remain stable across releases.
-keep enum com.example.agendasacramental.EstadoAgenda { *; }
-keep enum com.example.agendasacramental.TipoAsunto { *; }
-keep enum com.example.agendasacramental.TipoMensaje { *; }

# The history importer calls annotated methods from JavaScript.
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
