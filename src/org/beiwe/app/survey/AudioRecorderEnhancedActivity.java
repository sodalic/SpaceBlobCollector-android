package org.beiwe.app.survey;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.beiwe.app.CrashHandler;
import org.beiwe.app.storage.AudioFileManager;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;

//http://www.edumobile.org/android/audio-recording-in-wav-format-in-android-programming/

public class AudioRecorderEnhancedActivity extends AudioRecorderCommon{
	//WAV stuff
	private static final int BIT_DEPTH = 16;
	private static final int SAMPLE_RATE = 44100; //TODO: set to per survey value.
	
	private int BUFFER_SIZE = 0; //constant set in onCreate
	
	private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
	private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
	private static final int IS_INITIALIZED = AudioRecord.STATE_INITIALIZED;
	
	public static final String unencryptedRawAudioFileName = "unencryptedRawAudioFile";
	private static String unencryptedRawAudioFilePath;
	
	@Override
    protected String getFileExtension() { return ".wav"; }

	private AudioRecord recorder = null;	
	private Thread recordingThread = null;
//	private Thread copyAndEncryptThread = null;

	@Override
	public void onCreate( Bundle savedInstanceState ) {
		super.onCreate( savedInstanceState );
		unencryptedRawAudioFilePath = getApplicationContext().getFilesDir().getAbsolutePath() + "/" + unencryptedRawAudioFileName;
		
		//TODO: implement extracting sample rate from survey parameters
//		SAMPLE_RATE = 
		
		BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE,
				AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT );
	}
	
	/*#############################################################
	 ########################### WAV ##############################
	 ############################################################*/
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		if ( isFinishing() ) { // If the activity is being finished()...
	        if (recorder != null) { stopRecording(); }
		}
	}

    /*#########################################################
    ################# Recording and Playing ################### 
    #########################################################*/

    /** Starts playing back the recording */
    @Override protected void startPlaying() { super.startPlaying(); }
    @Override protected void stopPlaying() { super.stopPlaying(); }
    
    /** Start recording from the device's microphone */
    @Override
    protected void startRecording() {
    	super.startRecording();
    	//recording stuff
		recorder = new AudioRecord( MediaRecorder.AudioSource.MIC,
				SAMPLE_RATE, RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING, BUFFER_SIZE );
		if ( recorder.getState() == IS_INITIALIZED ) { recorder.startRecording(); }
		else { stopRecording(); //if it doesn't work, fail gracefully?
		       Log.e("enhanced audio", "audio recording failed to initialize?"); } 
		//Start recording.
		recordingThread = new Thread( new Runnable() {
			@Override public void run() { writeAudioDataToFile(); }
		}, "AudioRecorder Thread");
		recordingThread.start();
		startRecordingTimeout();  //TODO: test recording timeout with enhanced recording...
    }
    
    /** Stop recording, and reset the button to "record" */
    @Override
    public void stopRecording() {
    	super.stopRecording();
    	if ( recorder != null) {
    		currentlyRecording = false;
    		if ( recorder.getState() == IS_INITIALIZED ) { recorder.stop(); }
    		recorder.release();
    		recorder = null;
    		recordingThread = null;
    	}
    	//TODO: is it too much of a hassle to stick these on a separate thread?
    	//We need to do this because the the RAW file is not formatted correctly for playback
    	AudioFileManager.copyToWaveFile( unencryptedRawAudioFilePath, unencryptedTempAudioFilePath,                     
                     					 SAMPLE_RATE, BIT_DEPTH, BUFFER_SIZE );
    	AudioFileManager.delete(unencryptedRawAudioFileName);
        // Encrypt the audio file as soon as recording is finished
        new EncryptAudioFileTask().execute();
    }
    
    /**Writes data from the AudioRecord to a file.
     * This function is much harder to run as code outside of enhanced audio recording activity,
     * so we are going to keep it here.
     * This function blocks until currentlyRecording gets set to false, so run on a separate threod. */
	private void writeAudioDataToFile() {
		int recordingStatus = 0;
		byte data[] = new byte[BUFFER_SIZE];
		FileOutputStream rawAudioFile = null;
		//setup file.
		try { rawAudioFile = new FileOutputStream( unencryptedRawAudioFilePath ); }
		catch (FileNotFoundException e) { CrashHandler.writeCrashlog(e, getApplicationContext() ); return; }
		//while recording get audio data chunks.
		while ( currentlyRecording ) {
			recordingStatus = recorder.read(data, 0, BUFFER_SIZE);
			if ( recordingStatus != AudioRecord.ERROR_INVALID_OPERATION ) {
				try { rawAudioFile.write(data); }
				catch (IOException e) { e.printStackTrace(); } //swallow error.
			}
		}
		try { rawAudioFile.close(); }
		catch (IOException e) { e.printStackTrace(); }
	}    
}