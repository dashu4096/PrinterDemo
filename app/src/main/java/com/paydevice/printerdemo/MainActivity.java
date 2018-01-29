package com.paydevice.printerdemo;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.paydevice.printerdemo.printer.PrinterException;
import com.paydevice.printerdemo.printer.PrinterManager;
import com.paydevice.printerdemo.printer.SerialPortPrinter;
import com.paydevice.printerdemo.printer.UsbPrinter;
import com.paydevice.printerdemo.template.PosSalesSlip;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "PrinterDemo";
	private static final String KEY_PRINTING_RESULT  = "printing_result";
    private static final String KEY_TEMPLATE_ID  = "template_id";
	private static final int MSG_PRINTING_RESULT  = 1;
    private static final int USB_TEMPLATE_ID = 1;
    private static final int SERIAL_TEMPLATE_ID = 2;

	private PrinterManager mUsbPrinterManager = null;
	private PrinterManager mSerialPrinterManager = null;
	private PosSalesSlip mUsbPrintTemplate = null;
	private PosSalesSlip mSerialPrintTemplate = null;
	private PrintTask mUsbPrintTask = null;
	private PrintTask mSerialPrintTask = null;

	//USB printer
    private Spinner mUsbSpinner;
    private UsbListAdapter mUsbAdapter;
	private UsbPrinter mUsbPrinter = null;
    private List<UsbDevice> mUsbList = null;

	//Serial port printer
    private Button mSerialBtn;
    private Spinner mSerialPathSpinner;
    private ArrayAdapter<CharSequence> mSerialPathAdapter;
    private Spinner mSerialSpeedSpinner;
    private ArrayAdapter<CharSequence> mSerialSpeedAdapter;
    private SerialPortPrinter mSerialPrinter = null;


    private final BroadcastReceiver mUsbStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null) {
				if (action.equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
                    Log.d(TAG, "USB device attached");
                } else {
                    Log.d(TAG, "USB device detached");
                }
				refreshUsbList();
            }
        }
    };

    private final Handler mHandler = new MyHandler(this);

    static class MyHandler extends Handler {
        private final WeakReference<MainActivity> mActivity;

        public MyHandler(Activity activity) {
            mActivity = new WeakReference<MainActivity>((MainActivity) activity);
        }
        @Override
        public void handleMessage(Message msg) {
            if(mActivity.get() != null) {
                switch(msg.what) {
                    case MSG_PRINTING_RESULT:
                        final Boolean result = msg.getData().getBoolean(KEY_PRINTING_RESULT);
                        if (result) {
                            Toast.makeText(mActivity.get(), R.string.printer_printing_success, Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(mActivity.get(), R.string.printer_printing_failed, Toast.LENGTH_SHORT).show();
                        }
                        final int id = msg.getData().getInt(KEY_TEMPLATE_ID);
                        if (id == USB_TEMPLATE_ID && mActivity.get().mUsbPrintTemplate != null) {
                            mActivity.get().mUsbPrintTemplate.destroy();
                        }
                        if (id == SERIAL_TEMPLATE_ID && mActivity.get().mSerialPrintTemplate != null) {
                            mActivity.get().mSerialPrintTemplate.destroy();
                        }
                        break;

                    default:
                        super.handleMessage(msg);
                        break;
                }
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initUsb();
        initSerial();
    }

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mHandler.removeCallbacksAndMessages(null);
		unregisterReceiver(mUsbStatusReceiver);
		cleanPrintTask();
	}

	private void initUsb() {
        //register USB action
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(mUsbStatusReceiver, filter);

        refreshUsbList();

        mUsbSpinner = (Spinner) findViewById(R.id.usb_spinner);
        mUsbSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "select usb printer:" + mUsbList.get(position).getProductName()
                        + "-" + mUsbList.get(position).getSerialNumber());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        mUsbSpinner.setAdapter(mUsbAdapter);
    }

    private void initSerial() {
		if (mSerialPrinter == null) {
			mSerialPrinter = new SerialPortPrinter();
			//58mm serial port printer
			mSerialPrinterManager = new PrinterManager(mSerialPrinter, PrinterManager.TYPE_PAPER_WIDTH_58MM);
		}
        final String[] mPathList = mSerialPrinter.getAllDevicesPath();
        mSerialPathAdapter = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item, mPathList);

        mSerialBtn = (Button) findViewById(R.id.serial_btn);
        mSerialPathSpinner = (Spinner) findViewById(R.id.serial_path);
        mSerialPathSpinner.setAdapter(mSerialPathAdapter);
        mSerialPathSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "select serial:"+mSerialPathAdapter.getItem(position).toString());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        mSerialSpeedAdapter = ArrayAdapter.createFromResource(this, R.array.serial_speed, android.R.layout.simple_spinner_item);

        mSerialSpeedSpinner = (Spinner) findViewById(R.id.serial_speed);
        mSerialSpeedSpinner.setAdapter(mSerialSpeedAdapter);
        mSerialSpeedSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "select baudrate:"+mSerialSpeedAdapter.getItem(position).toString());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

	public void onUsbBtnClick(View view) {
		if (mUsbList != null && mUsbList.size() > 0) {
			final UsbDevice selectUsbDev = mUsbList.get(mUsbSpinner.getSelectedItemPosition());
			if (mUsbPrinter == null) {
				mUsbPrinter = new UsbPrinter(this);
				//80mm USB printer
				mUsbPrinterManager = new PrinterManager(mUsbPrinter, PrinterManager.TYPE_PAPER_WIDTH_80MM);
			}
			mUsbPrinter.selectPrinter(selectUsbDev);
			if (mUsbPrintTemplate == null) {
				mUsbPrintTemplate = new PosSalesSlip(this, mUsbPrinterManager);
				mUsbPrintTemplate.setTemplateId(USB_TEMPLATE_ID);
			}
			int errCode = mSerialPrintTemplate.prepare();
			if (errCode == 0) {
				mUsbPrintTemplate.setMerchantName("CocoPark");
				mUsbPrintTemplate.setMerchantNo("102510154110045");
				mUsbPrintTemplate.setTerminalNo("10095066");
				mUsbPrintTemplate.setOperatorNo("01");
				mUsbPrintTemplate.setIssuer("03050001");
				mUsbPrintTemplate.setCardNo("622602******9376");
				mUsbPrintTemplate.setTxnType("SALE");
				mUsbPrintTemplate.setBatchNo("000814");
				mUsbPrintTemplate.setVoucherNo("003707");
				mUsbPrintTemplate.setAuthNo("381936");
				mUsbPrintTemplate.setExpDate("0000");
				mUsbPrintTemplate.setRefNo("103758494052");
				mUsbPrintTemplate.setDate("2012-11-25 10:37:58");
				mUsbPrintTemplate.setAmount("67.10");

				mUsbPrintTask = new PrintTask();
				mUsbPrintTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mUsbPrintTemplate);
			} else {
				Log.d(TAG,"prepare error:"+errCode);
			}
		}
	}

	public void onSerialBtnClick(View view) {
		if (mSerialPathAdapter != null) {
		    final String path = mSerialPathAdapter.getItem(mSerialPathSpinner.getSelectedItemPosition()).toString();
		    final int speed = Integer.parseInt(mSerialSpeedAdapter.getItem(mSerialSpeedSpinner.getSelectedItemPosition()).toString());
			if (mSerialPrinter == null) {
				mSerialPrinter = new SerialPortPrinter();
				//58mm serial port printer
				mSerialPrinterManager = new PrinterManager(mSerialPrinter, PrinterManager.TYPE_PAPER_WIDTH_58MM);
			}
			mSerialPrinter.selectPrinter(path, speed);
			if (mSerialPrintTemplate == null) {
				mSerialPrintTemplate = new PosSalesSlip(this, mSerialPrinterManager);
				mSerialPrintTemplate.setTemplateId(SERIAL_TEMPLATE_ID);
			}
			int errCode = mSerialPrintTemplate.prepare();
			if (errCode == 0) {
				mSerialPrintTemplate.setMerchantName("CocoPark");
				mSerialPrintTemplate.setMerchantNo("102510154110045");
				mSerialPrintTemplate.setTerminalNo("10095066");
				mSerialPrintTemplate.setOperatorNo("01");
				mSerialPrintTemplate.setIssuer("03050001");
				mSerialPrintTemplate.setCardNo("622602******9376");
				mSerialPrintTemplate.setTxnType("SALE");
				mSerialPrintTemplate.setBatchNo("000814");
				mSerialPrintTemplate.setVoucherNo("003707");
				mSerialPrintTemplate.setAuthNo("381936");
				mSerialPrintTemplate.setExpDate("0000");
				mSerialPrintTemplate.setRefNo("103758494052");
				mSerialPrintTemplate.setDate("2012-11-25 10:37:58");
				mSerialPrintTemplate.setAmount("67.10");

				mSerialPrintTask = new PrintTask();
				mSerialPrintTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mSerialPrintTemplate);
			} else {
				Log.d(TAG,"prepare error:"+errCode);
			}
        }
    }

	private void cleanPrintTask() {
		Log.d(TAG,"cleanPrintTask");
		if (mUsbPrintTask != null && mUsbPrintTask.getStatus() != AsyncTask.Status.FINISHED) {
			mUsbPrintTask.cancel(true);
			mUsbPrintTask = null;
		}
		if (mUsbPrintTemplate != null) {
			mUsbPrintTemplate.destroy();
            mUsbPrintTemplate = null;
		}

		if (mSerialPrintTask != null && mSerialPrintTask.getStatus() != AsyncTask.Status.FINISHED) {
			mSerialPrintTask.cancel(true);
			mSerialPrintTask = null;
		}
		if (mSerialPrintTemplate != null) {
			mSerialPrintTemplate.destroy();
            mSerialPrintTemplate = null;
		}
	}

	private class PrintTask extends AsyncTask<PosSalesSlip, Void, Boolean> {
        private int mId;
		protected Boolean doInBackground(PosSalesSlip...templates) {
			if (isCancelled()) {
				return false;
			}
			try {
				if (templates[0].validate()) {
				    mId = templates[0].getTemplateId();
					templates[0].print();
				} else {
					Log.d(TAG,"Template data illegal!");
					return false;
				}
			} catch (PrinterException e) {
				Log.d(TAG, "print failed code:"+e.getErrorCode());
				return false;
			}
			return true;
		}

		protected void onPostExecute(Boolean result) {
			Bundle bundle = new Bundle();
            Message msg = new Message();

            bundle.putBoolean(KEY_PRINTING_RESULT, result);
            bundle.putInt(KEY_TEMPLATE_ID, mId);
			msg.what = MSG_PRINTING_RESULT;
			msg.setData(bundle);
			mHandler.sendMessage(msg);
		}
	}

    private void refreshUsbList() {
		if (mUsbPrinter == null) {
			mUsbPrinter = new UsbPrinter(this);
			//80mm USB printer
			mUsbPrinterManager = new PrinterManager(mUsbPrinter, PrinterManager.TYPE_PAPER_WIDTH_80MM);
		}
        if (mUsbList != null) {
            mUsbList.clear();
        }
        mUsbList = mUsbPrinter.getPrinterList();

        if (mUsbAdapter == null) {
            mUsbAdapter = new UsbListAdapter(this, mUsbList);
        } else {
            mUsbAdapter.refresh(mUsbList);
        }
    }

    private static class UsbListAdapter extends BaseAdapter {
        private List<UsbDevice> mList;
        private final LayoutInflater mInflater;

        public UsbListAdapter(Context context, final List<UsbDevice> list) {
            mInflater = LayoutInflater.from(context);
            mList = (list != null) ? list : new ArrayList<UsbDevice>();
        }

        public void refresh(List<UsbDevice> list) {
            mList = list;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return mList.size();
        }

        @Override
        public UsbDevice getItem(final int position) {
            if ((position >= 0) && (position < mList.size()))
                return mList.get(position);
            else
                return null;
        }

        @Override
        public long getItemId(final int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, final ViewGroup parent) {
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.listitem, parent, false);
            }
            if (convertView instanceof TextView) {
                final UsbDevice device = getItem(position);
                TextView text = convertView.findViewById(R.id.name);
                text.setText(String.format("%s(%s)", device.getProductName(), device.getSerialNumber()));
            }
            return convertView;
        }
    }
}
