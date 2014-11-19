/*    */ package com.ibm.tivoli.maximo.mobile.android.sensor.barcode.camera.zxing;
/*    */ 
/*    */ import android.content.Intent;
/*    */ import com.ibm.tivoli.maximo.mobile.android.sensor.barcode.camera.AndroidCameraBarcodeReader;
/*    */ import com.mro.mobile.sensor.barcode.MobileBarcodeEvent;
/*    */ 
/*    */ public class ZXingIntentBarcodeReader
/*    */   extends AndroidCameraBarcodeReader
/*    */ {
/*    */   public MobileBarcodeEvent getMobileBarcodeEventResultOnSuccess(int resultCode, Intent intent)
/*    */   {
/* 26 */     String contents = intent.getStringExtra("SCAN_RESULT");
/* 27 */     return new MobileBarcodeEvent(intent, contents);
/*    */   }
/*    */   
/*    */   public Intent getBarcodeIntentSettings()
/*    */   {
/* 32 */     Intent intent = new Intent("com.google.zxing.client.android.SCAN");
/* 33 */     intent.addCategory("android.intent.category.DEFAULT");
/* 34 */     intent.putExtra("SCAN_WIDTH", 800);
/* 35 */     intent.putExtra("SCAN_HEIGHT", 200);
/* 36 */     intent.putExtra("RESULT_DISPLAY_DURATION_MS", 2000L);
/* 37 */     intent.putExtra("PROMPT_MESSAGE", "");
/* 38 */     return intent;
/*    */   }
/*    */   
/*    */   public MobileBarcodeEvent getMobileBarcodeEventResultOnError(int resultCode, Intent intentResult)
/*    */   {
/* 43 */     return new MobileBarcodeEvent(new Exception("Error code received from barcode reader: " + resultCode));
/*    */   }
/*    */ }


/* Location:           C:\Users\timminsa\Documents\BBC\Code\maximo-mobile-classes\
 * Qualified Name:     com.ibm.tivoli.maximo.mobile.android.sensor.barcode.camera.zxing.ZXingIntentBarcodeReader
 * JD-Core Version:    0.7.0.1
 */