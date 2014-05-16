/**
 *  Catroid: An on-device visual programming system for Android devices
 *  Copyright (C) 2010-2013 The Catrobat Team
 *  (<http://developer.catrobat.org/credits>)
 *  
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation, either version 3 of the
 *  License, or (at your option) any later version.
 *  
 *  An additional term exception under section 7 of the GNU Affero
 *  General Public License, version 3, is available at
 *  http://developer.catrobat.org/license_additional_term
 *  
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU Affero General Public License for more details.
 *  
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.catrobat.catroid.stage;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.parrot.freeflight.receivers.DroneConnectionChangeReceiverDelegate;
import com.parrot.freeflight.receivers.DroneConnectionChangedReceiver;
import com.parrot.freeflight.receivers.DroneReadyReceiver;
import com.parrot.freeflight.receivers.DroneReadyReceiverDelegate;
import com.parrot.freeflight.service.DroneControlService;

import org.catrobat.catroid.BuildConfig;
import org.catrobat.catroid.drone.DroneServiceWrapper;

public class DroneStageListener implements StageListenerInterface, DroneReadyReceiverDelegate,
		DroneConnectionChangeReceiverDelegate {

	private Context stageActivityContext = null;
	private Intent stageStartIntent = null;

	private Boolean droneIsRequired = false;

	private static final String TAG = DroneStageListener.class.getSimpleName();

	protected DroneControlService droneControlService = null;
	private BroadcastReceiver droneReadyReceiver = null;
	private DroneConnectionChangedReceiver droneConnectionChangeReceiver = null;

	public DroneStageListener(Context stageActivityContext, Intent stageStartIntent) {
		this.stageActivityContext = stageActivityContext;
		this.stageStartIntent = stageStartIntent;
	}

	@Override
	public void onCreate() {
		//TODO Drone: process reuturn value
		if (prepareDroneRessources()) {
			Log.d(TAG, "Failure during drone service startup");
		}
	}

	@Override
	public void onResume() {
		if (BuildConfig.DEBUG) {
			if (droneControlService != null) {
				Log.d(TAG, "droneControlService .. onResume");
				droneControlService.resume();
				DroneServiceWrapper.getInstance().setDroneService(droneControlService);
			}
			LocalBroadcastManager manager = LocalBroadcastManager.getInstance(stageActivityContext);
			manager.registerReceiver(droneReadyReceiver, new IntentFilter(DroneControlService.DRONE_STATE_READY_ACTION));
			manager.registerReceiver(droneConnectionChangeReceiver, new IntentFilter(
					DroneControlService.DRONE_CONNECTION_CHANGED_ACTION));

		}
	}

	@Override
	public void onPause() {
		if (BuildConfig.DEBUG) {
			if (droneControlService != null) {
				droneControlService.pause();
				DroneServiceWrapper.getInstance().setDroneService(null);
			}
			LocalBroadcastManager manager = LocalBroadcastManager.getInstance(stageActivityContext);
			manager.unregisterReceiver(droneReadyReceiver);
			manager.unregisterReceiver(droneConnectionChangeReceiver);
		}
	}

	@Override
	public void onDestroy() {
		helpUnbindDroneService();
	}

	private boolean prepareDroneRessources() {
		if (BuildConfig.DEBUG) {
			droneIsRequired = stageStartIntent.getBooleanExtra(PreStageActivity.INIT_DRONE_STRING_EXTRA, false);
			Log.d(TAG, "prepareRessources() initDrone=" + droneIsRequired.toString());
			if (droneIsRequired) {
				droneReadyReceiver = new DroneReadyReceiver(this);
				droneConnectionChangeReceiver = new DroneConnectionChangedReceiver(this);

				helpBindDroneService();
			}
			return true;
		}
		return false;
	}

	private void onDroneServiceConnected(IBinder service) {
		Log.d(TAG, "onDroneServiceConnected");
		droneControlService = ((DroneControlService.LocalBinder) service).getService();
		DroneServiceWrapper.getInstance().setDroneService(droneControlService);
		droneControlService.resume();
		droneControlService.requestDroneStatus();
		droneControlService.requestConfigUpdate();

		Log.d(TAG, "DroneServiceConnection");
	}

	private ServiceConnection droneServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			Log.d(TAG, "Drone Connected");
			onDroneServiceConnected(service);
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			Log.d(TAG, "Drone Disconnected");
			droneControlService = null;
		}

	};

	private void helpUnbindDroneService() {
		if (droneControlService != null) {
			stageActivityContext.unbindService(droneServiceConnection);
			droneServiceConnection = null;
			droneControlService = null;
		}
	}

	private boolean helpBindDroneService() {
		boolean droneServiceWasCreated = false;
		if (droneControlService == null) {
			droneServiceWasCreated = stageActivityContext.bindService(new Intent(stageActivityContext,
					DroneControlService.class), this.droneServiceConnection, Context.BIND_AUTO_CREATE);
			if (!droneServiceWasCreated) {
				Log.d(TAG, "Connection to the drone not successful");
			}
		}
		return droneServiceWasCreated;
	}

	@Override
	public void onDroneReady() {
		Log.d(TAG, "onDroneReady");
	}

	@Override
	public void onDroneConnected() {
		Log.d(TAG, "onDroneConnected");
		droneControlService.requestConfigUpdate();
	}

	@Override
	public void onDroneDisconnected() {
		Log.d(TAG, "onDroneDisconnected");
		//Nothing to do here
	}
}