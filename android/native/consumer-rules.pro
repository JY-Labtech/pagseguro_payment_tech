# Consumer ProGuard rules para o AAR do Pagseguro Payment Tech.
# Informamos ao R8 para ignorar anotações que são removidas em tempo de compilação.

-dontwarn com.google.errorprone.annotations.**
-dontwarn kotlinx.android.parcel.Parcelize

# Modelos serializados com Gson e enviados ao Dart (onFinishedResponse/writeToFile/onTransactionInfo).
# Sem estas regras o R8 do app (minify em release) renomeia/remove os campos e o JSON chega com
# chaves ofuscadas, fazendo o PagseguroResponseModel ficar vazio.
-keep class br.com.jylabtech.pagseguro_payment_tech.core.BaseResult { <fields>; }
-keep class br.com.jylabtech.pagseguro_payment_tech.core.ActionResult { <fields>; }
-keep class br.com.jylabtech.pagseguro_payment_tech.core.UserDataResult { <fields>; }
