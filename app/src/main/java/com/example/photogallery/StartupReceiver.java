package com.example.photogallery;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class StartupReceiver extends BroadcastReceiver {
	private static final String TAG="startupReceiver";
	@Override
	public void onReceive(Context context, Intent intent) {
		boolean isOn=QueryPreferences.isAlarmOn(context);
		PollService.setServiceAlarm(context,isOn);
	}
}
