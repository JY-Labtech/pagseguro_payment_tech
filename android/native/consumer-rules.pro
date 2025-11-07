# Consumer ProGuard rules para o AAR do Pagseguro Payment Tech.
# Informamos ao R8 para ignorar anotações que são removidas em tempo de compilação.

-dontwarn com.google.errorprone.annotations.**
-dontwarn kotlinx.android.parcel.Parcelize
