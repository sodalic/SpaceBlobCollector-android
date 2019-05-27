package org.beiwe.app.ui.user;

import io.sodalic.blob.R;
import org.beiwe.app.session.SessionActivity;
import org.beiwe.app.storage.PersistentData;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

/**The main menu activity of the app. Currently displays 4 buttons - Audio Recording, Graph, Call Clinician, and Sign out.
 * @author Dor Samet */
public class MainMenuActivity extends SessionActivity {
	//extends a SessionActivity
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main_menu);

		Button callClinicianButton = (Button) findViewById(R.id.main_menu_call_clinician);
		if(PersistentData.getCallClinicianButtonEnabled()) {
			callClinicianButton.setText(PersistentData.getCallClinicianButtonText());
		}
		else {
			callClinicianButton.setVisibility(View.GONE);
		}
	}
	
	/*#########################################################################
	############################## Buttons ####################################
	#########################################################################*/
	
//	public void graphResults (View v) { startActivity( new Intent(getApplicationContext(), GraphActivity.class) ); }
}
