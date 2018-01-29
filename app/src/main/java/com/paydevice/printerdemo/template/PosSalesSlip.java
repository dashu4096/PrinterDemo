package com.paydevice.printerdemo.template;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.text.TextUtils;
import android.util.Log;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.WriterException;
import com.paydevice.printerdemo.R;
import com.paydevice.printerdemo.printer.PrinterCommon;
import com.paydevice.printerdemo.printer.PrinterException;
import com.paydevice.printerdemo.printer.PrinterManager;

import java.util.Locale;


/**
* @brief a printing template
*/
public class PosSalesSlip {

	private String dividingLine;
	/** Printer object */
	private PrinterManager mPrinterManager;
	/** Application context */
	private Context mContext;

	private String merchantName;
	private String merchantNo;
	private String operatorNo;
	private String terminalNo;
	private String issuer;
	private String acquirer;
	private String cardNo;
	private String txnType;
	private String batchNo;
	private String voucherNo;
	private String authNo;
	private String expDate;
	private String refNo;
	private String date;
	private String amount;
	private String total;
	private String reference;
	//private String cardholderSign;
	private String phone;

    private int mTemplateId;

	public PosSalesSlip(Context context, PrinterManager printer) {
		mContext = context;
		mPrinterManager = printer;
	}

	/**
	* @brief initialize printer and check the paper
	*
	* @return 0: success other: error code
	*/
	public int prepare() {
		try {
			mPrinterManager.connect();
			if (mPrinterManager.getPrinterType() == PrinterCommon.SERIAL_PRINTER) {
				mPrinterManager.checkPaper();
			}
        } catch (PrinterException e) {
		    if (e.getErrorCode() == PrinterCommon.ERR_NO_PAPER) {
		        destroy();
            }
            return e.getErrorCode();
        }
		return 0;
	}

	/**
	* @brief deinitialize printer
	*
	* @return 
	*/
	public int destroy() {
		try {
			mPrinterManager.disconnect();
		} catch (PrinterException e) {
			return e.getErrorCode();
		}
		return 0;
	}

	/**
	* @brief print template
	*
	* @return 
	*/
	synchronized public void print() throws PrinterException {
		//feed few lines to avoid some printer no clear at head
		mPrinterManager.cmdLineFeed();
		
		//test for PT486
		if (mPrinterManager.getPrinterType() == PrinterCommon.SERIAL_PRINTER) {
			//mPrinterManager.cmdSetHeatingParam(9, 130, 10);
			mPrinterManager.cmdSetHeatingParam(7, 110, 2);
			languageTest();
		}

		//print logo
		//Note: the bmp must be 24bit(RGB888), width<dots_per_line. please resize the bmp size to small if printing bmp have issue
		Bitmap logo = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.logo);
		int x_offset = (mPrinterManager.getDotsPerLine() - logo.getWidth() ) >> 1;//horizontal centre
		int y_offset = 0;
		mPrinterManager.cmdBitmapPrint(bitmap2Binary(logo), PrinterManager.BITMAP_ZOOM_NONE, x_offset, y_offset, 150);
		mPrinterManager.cmdLineFeed();

		Locale locale = mContext.getResources().getConfiguration().locale;
		String language = locale.getLanguage();
		if (language.endsWith("zh")) {
			//NOTE:some control broad only support "ESC R n"
			if (mPrinterManager.getPrinterType() == PrinterCommon.SERIAL_PRINTER) {
				mPrinterManager.cmdSetCodePageTable(PrinterManager.CODE_PAGE_GB18030);
			} else {
				mPrinterManager.cmdSetCharacterSet(PrinterManager.CHAR_SET_CHINA);
			}
			mPrinterManager.setStringEncoding("GB18030");
		} else {
			//NOTE:some control broad only support "ESC R n"
			if (mPrinterManager.getPrinterType() == PrinterCommon.SERIAL_PRINTER) {
				mPrinterManager.cmdSetCodePageTable(PrinterManager.CODE_PAGE_CP437);
			} else {
				mPrinterManager.cmdSetCharacterSet(PrinterManager.CHAR_SET_ENGLAND);
			}
			mPrinterManager.setStringEncoding("CP437");
		}
		
		//print template
		dividingLine = makeDividingLine();
		mPrinterManager.cmdSetPrintMode(PrinterManager.FONT_DOUBLE_HEIGHT
				|PrinterManager.FONT_DOUBLE_WIDTH
				|PrinterManager.FONT_EMPHASIZED);
		mPrinterManager.cmdSetAlignMode(PrinterManager.ALIGN_MIDDLE);
		mPrinterManager.sendData(mContext.getString(R.string.tag_title));
		mPrinterManager.cmdLineFeed(2);
		//restore default font style
		mPrinterManager.cmdSetPrintMode(PrinterManager.FONT_DEFAULT);
		mPrinterManager.cmdSetAlignMode(PrinterManager.ALIGN_LEFT);
		mPrinterManager.sendData(mContext.getString(R.string.tag_page_header));
		mPrinterManager.cmdLineFeed();
		mPrinterManager.sendData(dividingLine);
		mPrinterManager.cmdLineFeed();
		mPrinterManager.sendData(stringAlignLeftRight(mContext.getString(R.string.tag_merchant_name),merchantName));
		mPrinterManager.cmdLineFeed();
		mPrinterManager.sendData(stringAlignLeftRight(mContext.getString(R.string.tag_merchant_no),merchantNo));
		mPrinterManager.cmdLineFeed();
		mPrinterManager.sendData(stringAlignLeftRight(mContext.getString(R.string.tag_terminal_no),terminalNo));
		mPrinterManager.cmdLineFeed();
		mPrinterManager.sendData(stringAlignLeftRight(mContext.getString(R.string.tag_date),date));
		mPrinterManager.cmdLineFeed();
		mPrinterManager.sendData(stringAlignLeftRight(mContext.getString(R.string.tag_issuer),issuer));
		mPrinterManager.cmdLineFeed();
		mPrinterManager.sendData(stringAlignLeftRight(mContext.getString(R.string.tag_card_no),cardNo));
		mPrinterManager.cmdLineFeed();
		mPrinterManager.sendData(stringAlignLeftRight(mContext.getString(R.string.tag_exp_date),expDate));
		mPrinterManager.cmdLineFeed();
		mPrinterManager.sendData(stringAlignLeftRight(mContext.getString(R.string.tag_txn_type),txnType));
		mPrinterManager.cmdLineFeed();
		mPrinterManager.sendData(stringAlignLeftRight(mContext.getString(R.string.tag_batch_no),batchNo));
		mPrinterManager.cmdLineFeed();
		mPrinterManager.sendData(stringAlignLeftRight(mContext.getString(R.string.tag_voucher_no),voucherNo));
		mPrinterManager.cmdLineFeed();
		mPrinterManager.sendData(stringAlignLeftRight(mContext.getString(R.string.tag_ref_no),refNo));
		mPrinterManager.cmdLineFeed();
		mPrinterManager.sendData(stringAlignLeftRight(mContext.getString(R.string.tag_auth_no),authNo));
		mPrinterManager.cmdLineFeed();
		mPrinterManager.sendData(stringAlignLeftRight(mContext.getString(R.string.tag_operator_no),operatorNo));
		mPrinterManager.cmdLineFeed();
		mPrinterManager.sendData(stringAlignLeftRight(mContext.getString(R.string.tag_amount),amount));
		mPrinterManager.cmdLineFeed();
		mPrinterManager.sendData(mContext.getString(R.string.tag_reference));
		mPrinterManager.cmdLineFeed();
		mPrinterManager.sendData(dividingLine);
		mPrinterManager.cmdLineFeed();
		mPrinterManager.sendData(mContext.getString(R.string.tag_page_footer));
		mPrinterManager.cmdLineFeed();

		//print barcode
		mPrinterManager.cmdLineFeed(1);
		mPrinterManager.cmdSetBarCodeStringPosition(PrinterManager.CODEBAR_STRING_MODE_BELOW);
		mPrinterManager.cmdSetBarCodeWidth(3);
		mPrinterManager.cmdSetBarCodeHeight(60);
		//mPrinterManager.cmdSetAbsPosition(10);
		String barCodeString = "6922266445057";
		mPrinterManager.cmdBarCodePrint(PrinterManager.EAN13, barCodeString);
		mPrinterManager.cmdLineFeed();

		//print qrcode bitmap
		String qrCoderString = "http://www.google.com";
		//NOTE: width < dots_per_line, x_offset + width <= dots_per_line
		int width    = 384;
		int height   = 384;
		x_offset = (mPrinterManager.getDotsPerLine() - width ) >> 1;//horizontal centre
		Bitmap qrBitmap = createQrCodeBitmap(qrCoderString, width, height);
		//PT486 need more delay
		if (mPrinterManager.getPrinterType() == PrinterCommon.SERIAL_PRINTER) {
			mPrinterManager.cmdBitmapPrint(qrBitmap, PrinterManager.BITMAP_ZOOM_NONE, x_offset, y_offset, 150);
			//printing end
			mPrinterManager.cmdLineFeed(5);
		} else {
			mPrinterManager.cmdBitmapPrint(qrBitmap, PrinterManager.BITMAP_ZOOM_NONE, x_offset, y_offset);
			//printing end
			mPrinterManager.cmdLineFeed(5);
			mPrinterManager.cmdCutPaper(PrinterManager.FULL_CUT);
		}
	}

	public boolean validate() {
		if (TextUtils.isEmpty(merchantName)
				||TextUtils.isEmpty(merchantNo)
				||TextUtils.isEmpty(terminalNo)
				||TextUtils.isEmpty(operatorNo)
				||TextUtils.isEmpty(issuer)
				||TextUtils.isEmpty(cardNo)
				||TextUtils.isEmpty(txnType)
				||TextUtils.isEmpty(batchNo)
				||TextUtils.isEmpty(voucherNo)
				||TextUtils.isEmpty(authNo)
				||TextUtils.isEmpty(expDate)
				||TextUtils.isEmpty(refNo)
				||TextUtils.isEmpty(date)
				||TextUtils.isEmpty(amount)){
				return false;
		}
		return true;
	}

	//debug
	synchronized public void languageTest() throws PrinterException {
		mPrinterManager.setStringEncoding("CP437");
		mPrinterManager.cmdSetCodePageTable(PrinterManager.CODE_PAGE_CP437);
		mPrinterManager.cmdLineFeed(1);
		mPrinterManager.sendData("----English----");
		mPrinterManager.cmdLineFeed(1);
		mPrinterManager.sendData("abcdefghijklmnopqrstuvwxyz`1234567890-=~!@#$%^&*()_+[]{};':\",./<>?\\|");

		mPrinterManager.setStringEncoding("CP437");
		mPrinterManager.cmdSetCodePageTable(PrinterManager.CODE_PAGE_CP437);
		mPrinterManager.cmdLineFeed(1);
		mPrinterManager.sendData("-----German special---");
		mPrinterManager.cmdLineFeed(1);
		mPrinterManager.sendData("Ä ä Ö ö Ü ü ß");

		mPrinterManager.setStringEncoding("CP775");
		mPrinterManager.cmdSetCodePageTable(PrinterManager.CODE_PAGE_CP775);
		mPrinterManager.cmdLineFeed(1);
		mPrinterManager.sendData("----Latvian Alphabet----");
		mPrinterManager.cmdLineFeed(1);
		mPrinterManager.sendData("AĀBCČDEĒFGĢHIĪJKĶLĻMNŅOPRSŠTUŪVZŽ");
		mPrinterManager.cmdLineFeed(1);
		mPrinterManager.sendData("aābcčdeēfgģhiījkķlļmnņoprsštuūvzž");

		mPrinterManager.setStringEncoding("CP437");
		mPrinterManager.cmdSetCodePageTable(PrinterManager.CODE_PAGE_CP437);
		mPrinterManager.cmdLineFeed(1);
		mPrinterManager.sendData("---French----");
		mPrinterManager.cmdLineFeed(1);
		mPrinterManager.sendData("abcdefghijklmnopqrstuvwxyz");

		mPrinterManager.setStringEncoding("CP1251");
		mPrinterManager.cmdSetCodePageTable(PrinterManager.CODE_PAGE_CP1251);
		mPrinterManager.cmdLineFeed(1);
		mPrinterManager.sendData("----Russian Alphabet----");
		mPrinterManager.cmdLineFeed(1);
		mPrinterManager.sendData("Аа Бб Вв Гг Дд Ее Ёё Жж Зз Ии Йй Кк Лл† Мм Нн Оо");
		mPrinterManager.cmdLineFeed(1);
		mPrinterManager.sendData("Пп Рр Сс Тт Уу Фф Хх Цц Чч Шш Щщ Ъъ Ыы Ьь Ээ Юю Яя");

		mPrinterManager.setStringEncoding("CP874");
		mPrinterManager.cmdSetCodePageTable(PrinterManager.CODE_PAGE_CP874);
		mPrinterManager.cmdLineFeed(1);
		mPrinterManager.sendData("----Thai alphabet----");
		mPrinterManager.cmdLineFeed(1);
		mPrinterManager.sendData("ก ข ฃ ค ฅ ฆ ง จ ฉ ช ซ ฌ ญ ฎ ฏ ฐ ฑ ฒ ณ ด ต ถ ท ธ น บ ป ผ ฝ พ ฟ ภ ม ย ร ฤ ล ฦ ว ศ ษ ส ห ฬ อ ฮ ๐ ๑ ๒ ๓ ๔ ๕ ๖ ๗ ๘ ๙ ๚ ๛ ");

		mPrinterManager.setStringEncoding("CP1258");
		mPrinterManager.cmdSetCodePageTable(PrinterManager.CODE_PAGE_CP1258);
		mPrinterManager.cmdLineFeed(1);
		mPrinterManager.sendData("----Vietnamese alphabet----");
		mPrinterManager.cmdLineFeed(1);
		mPrinterManager.sendData("b o ô ơ a â ă ê e u ư i y r l c");

		mPrinterManager.setStringEncoding("CP864");
		mPrinterManager.cmdSetCodePageTable(PrinterManager.CODE_PAGE_CP864);
		mPrinterManager.cmdLineFeed(1);
		mPrinterManager.sendData("----Arabic Common----");
		mPrinterManager.cmdLineFeed(1);
		mPrinterManager.sendData("ﺎ ﺏ ﺕ ﺙ ﺝ ﺡ ﺥ ٠ ١ ٢ ٣ ٤ ٥ ٦ ٧ ٨ ٩");

		mPrinterManager.cmdSetCodePageTable(PrinterManager.CODE_PAGE_ISO_8859_2);
		mPrinterManager.cmdLineFeed(1);
		mPrinterManager.sendData("----Romanian Common----");
		mPrinterManager.cmdLineFeed(1);
		mPrinterManager.sendData("Ă ă Â â Î î Ş ş Ţ ţ","ISO-8859-2");//Ş ş Ţ ţ with cedilla

		//mPrinterManager.setStringEncoding("BIG5");
		mPrinterManager.cmdSetCodePageTable(PrinterManager.CODE_PAGE_BIG5);
		mPrinterManager.cmdLineFeed(1);
		mPrinterManager.sendData("----Traditional Chinese----");
		mPrinterManager.cmdLineFeed(1);
		mPrinterManager.sendData("繁體字","BIG5");

		//mPrinterManager.setStringEncoding("GB18030");
		mPrinterManager.cmdSetCodePageTable(PrinterManager.CODE_PAGE_GB18030);
		mPrinterManager.cmdLineFeed(1);
		mPrinterManager.sendData("----Simplified Chinese----");
		mPrinterManager.cmdLineFeed(1);
		mPrinterManager.sendData("简体字","GB18030");

		mPrinterManager.cmdLineFeed();
		mPrinterManager.cmdLineFeed(2);
	}

	/**
	* @brief create a bitmap by string(depend on zxing)
	*
	* @param str
	* @param w 0 - 383-x
	* @param h 0 - 383-y
	*
	* @return 
	*/
	public Bitmap createQrCodeBitmap(String str, int w, int h) {
		try {
			QRCodeWriter writer = new QRCodeWriter();
			BitMatrix matrix = writer.encode(str, BarcodeFormat.QR_CODE, w, h);
			//		int w = matrix.getWidth();
			//		int h = matrix.getHeight();
			int[] rawData = new int[w * h];
			for (int i = 0; i < w; i++) {
				for (int j = 0; j < h; j++) {
					int color = Color.WHITE;
					if (matrix.get(i, j)) {
						color = Color.BLACK;
					}
					rawData[i + (j * w)] = color;
				}
			}
			Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565);//16bit
			bitmap.setPixels(rawData, 0, w, 0, 0, w, h);

			return bitmap;
		} catch(WriterException e) {
			return null;
		}
	}

	public static Bitmap bitmap2Binary(Bitmap src) {
		int w, h;
		h = src.getHeight();
		w = src.getWidth();
		int[] pixels = new int[w*h];
		src.getPixels(pixels, 0, w, 0, 0, w, h);
		int alpha = 0xff<<24;
		for(int y=0;y<h;y++) {
			for(int x=0;x<w;x++) {
				int gray = pixels[w*y+x];
				int red = ((gray&0x00ff0000) >> 16);
				int green = ((gray&0x0000ff00) >> 8);
				int blue = ((gray&0x000000ff) >> 8);
				gray = (red + green + blue) / 3;
				gray = alpha | (gray << 16) | (gray << 8) | gray;
				pixels[w*y+x] = gray;
			}
		}
		Bitmap result = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565);
		result.setPixels(pixels, 0, w, 0, 0, w, h);
		return result;
	}

	private String makeDividingLine() {
		final int totalDots = mPrinterManager.getDotsPerLine();
		final int dotsPerAsciiChar = 12;
		StringBuilder result = new StringBuilder();
		int count = totalDots / dotsPerAsciiChar;
		for(int i=0;i<count;i++){
			result.append("-");
		}
		return result.toString();
	}

	//align right and left. usually,left string is locale language and right string is ascii.
	private String stringAlignLeftRight(String left, String right) {
		final int totalDots = mPrinterManager.getDotsPerLine();
		final int dotsPerAsciiChar = 12;
		int dotsPerChar = 12;
		Locale locale = mContext.getResources().getConfiguration().locale;
		String language = locale.getLanguage();
		if (language.endsWith("zh")) {
			dotsPerChar = 24;
		}

		StringBuilder result = new StringBuilder();
		int spaceCount = (totalDots - left.length()*dotsPerChar - right.length()*dotsPerAsciiChar)/dotsPerAsciiChar;
		result.append(left);
		//append space
		for(int i=0;i<spaceCount;i++){
			result.append(" ");
		}
		result.append(right);
		return result.toString();
	}

	public void setMerchantName(String str) {
		this.merchantName = str;
	}

	public String getMerchantName() {
		return merchantName;
	}

	public void setMerchantNo(String str) {
		this.merchantNo = str;
	}

	public String getMerchantNo() {
		return merchantNo;
	}

	public void setOperatorNo(String str) {
		this.operatorNo = str;
	}

	public String getOperatorNo() {
		return operatorNo;
	}

	public void setTerminalNo(String str) {
		this.terminalNo = str;
	}

	public String getTerminalNo() {
		return terminalNo;
	}

	public void setIssuer(String str) {
		this.issuer = str;
	}

	public String getIssuer() {
		return issuer;
	}

	public void setAcquirer(String str) {
		this.acquirer = str;
	}

	public String getAcquirer() {
		return acquirer;
	}

	public void setCardNo(String str) {
		this.cardNo = str;
	}

	public String getCardNo() {
		return cardNo;
	}

	public void setTxnType(String str) {
		this.txnType = str;
	}

	public String getTxnType() {
		return txnType;
	}

	public void setBatchNo(String str) {
		this.batchNo = str;
	}

	public String getBatchNo() {
		return batchNo;
	}

	public void setVoucherNo(String str) {
		this.voucherNo = str;
	}

	public String getVoucherNo() {
		return voucherNo;
	}

	public void setAuthNo(String str) {
		this.authNo = str;
	}

	public String getAuthNo() {
		return authNo;
	}

	public void setExpDate(String str) {
		this.expDate = str;
	}

	public String getExpDate() {
		return expDate;
	}

	public void setRefNo(String str) {
		this.refNo = str;
	}

	public String getRefNo() {
		return refNo;
	}

	public void setDate(String str) {
		this.date = str;
	}

	public String getDate() {
		return date;
	}

	public void setAmount(String str) {
		this.amount = str;
	}

	public String getAmount() {
		return amount;
	}

	public void setTotal(String str) {
		this.total = str;
	}

	public String getTotal() {
		return total;
	}

	public void setReference(String str) {
		this.reference = str;
	}

	public String getReference() {
		return reference;
	}

	public void setPhone(String str) {
		this.phone = str;
	}

	public String getPhone() {
		return phone;
	}


	public void setTemplateId(int id) { mTemplateId = id; }
	public int getTemplateId() { return mTemplateId; }
}
