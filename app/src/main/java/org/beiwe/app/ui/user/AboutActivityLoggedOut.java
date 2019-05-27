package org.beiwe.app.ui.user;

import io.sodalic.blob.R;
import org.beiwe.app.RunningBackgroundServiceActivity;
import org.beiwe.app.storage.PersistentData;

import android.os.Bundle;
import android.widget.TextView;

/**The about page!
 * @author Everyone! */
public class AboutActivityLoggedOut extends RunningBackgroundServiceActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_about);

		TextView aboutPageBody = (TextView) findViewById(R.id.about_page_body);
		aboutPageBody.setText(PersistentData.getAboutPageText());
	}
}
