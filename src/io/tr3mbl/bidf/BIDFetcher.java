package io.tr3mbl.bidf;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.json.JSONArray;
import org.json.JSONObject;

public class BIDFetcher {

      /////////////////////////////////////////////////////////////////////// 
     //                            VARIABLES                              // 
    /////////////////////////////////////////////////////////////////////// 
	
	// Used for UI
	public JFrame frmBundleidfetcher;
	private JPanel pnlHeader;
	private JPanel pnlContent;
	private JPanel pnlContentHeader;
	private JPanel pnlFooter;
	private JLabel lblFooter;
	private JLabel lblHeader;
	private JLabel labelContentHeader;
	private JButton btnGetImage;
	private JButton btnBatchGet;
	private JTextField txtBID;
    

    // Directory to save the images to
    public final String downloadDir = new File(ClassLoader.getSystemClassLoader().getResource(".").getPath()).getAbsolutePath() + "/BIDImages";


    // Variables for Batch-Get
    public List<String> missedBIDs = new ArrayList<String>();
    public int doneCount = 0;
    public int missedCount = 0;


    
      /////////////////////////////////////////////////////////////////////// 
     //                            MAIN FUNCTION                          // 
    /////////////////////////////////////////////////////////////////////// 
    
    // 
 	public static void main(String[] args) {
 		EventQueue.invokeLater(new Runnable() {
 			public void run() {
 				try {
 					BIDFetcher window = new BIDFetcher();
 					window.frmBundleidfetcher.setVisible(true);
 				} catch (Exception e) {
 					e.printStackTrace();
 				}
 			}
 		});
 	}
    
 	

      /////////////////////////////////////////////////////////////////////// 
     //                           OTHER METHODS                           // 
    /////////////////////////////////////////////////////////////////////// 
    
    // Called when new BIDFetcher is initialized
	public BIDFetcher() {
		// Create file from download directory
		File dir = new File(downloadDir);
		// Check if directory exists and if not make it
		if(!dir.exists())dir.mkdirs();
		
		// Method to initialize the GUI
		initialize();
	}

	
	// Downloads JSON from Apple's API
	private JSONObject getJSONFromURL(String sURL) {
		InputStream inputStream = null;
	    try {
	    	// Opens an InputStream from the given URl
	    	inputStream = new URL(sURL).openStream();
			// Initialize a Buffered Reader from the Input Stream with UTF-8 charset
	    	BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, Charset.forName("UTF-8")));
	    	// Initialize an empty String Builder
	    	StringBuilder stringBuilder = new StringBuilder();
	    	// Create variable to set the character code while reading the Buffered Reader
	        int charCode = -1;
	        // Loop through each character in Buffered Reader
	        while ((charCode = bufferedReader.read()) != -1) {
	        	// Add character to String Builder
	        	stringBuilder.append((char) charCode);
	        }
	        
	        // Return a new JSONObject from the JSON text
	        return new JSONObject(stringBuilder.toString());
	    } catch (Exception e) {
    		// Return null if error thrown while downloading JSON
			return null;
	    } finally {
	    	try {
	    		// Close the Input Stream
	    		inputStream.close();
			} catch (Exception e) {
	    		// Return null if cannot close the Input Stream
				return null;
			}
	    }
	}
	
	
	// Called when 'Get BundleID Image' is clicked
	private void getImage() {
		
		// Append the BundleID to Apple's API
		String url = "http:// itunes.apple.com/lookup?bundleId=" + txtBID.getText();
		// Get "results" JSONArray from the returned JSONObject
		JSONArray imageJSON = getJSONFromURL(url).getJSONArray("results");
		// Check if JSONArray is null and has children
		if(imageJSON != null && imageJSON.length() != 0) {
			try {
				// Download image from key "artworkUrl512" and show message box after downloading
				downloadImage(imageJSON.getJSONObject(0).getString("artworkUrl512"), txtBID.getText(), true);
			} catch (Exception e) {
				// Show message box if error thrown while downloading
				msgBox("Error while downloading image.");
				// Print stack trace
				e.printStackTrace();
			}
		} else {
			// Show message box if unable to find BundleID
			msgBox("Cannot find BundleID " + txtBID.getText() + ".");
		}
	}
	
	
	// Download image and show message box if wanted
	private void downloadImage(String sURL, String fileName, boolean showMsgBox) throws IOException {
		System.out.println("Downloading " + fileName + " from " + sURL);
		// Open an Input Stream from given URL
		try(InputStream in = new URL(sURL).openStream()){
			// Copy file from Input Stream to local file
		    Files.copy(in, Paths.get(downloadDir + "/" + fileName + ".jpg"), StandardCopyOption.REPLACE_EXISTING);
		}
		System.out.println("Finished downloading " + fileName);
		if(showMsgBox) {
			// Shows message box after downloading if needed
			msgBox("Finished downloading " + fileName);
		}
	}
	
	
	// Called when 'Select file for Batch-Get' is clicked 
	private void getBatch() {
		// Initializes a new JFileChooser starting in the image directory
		JFileChooser batchBIDGetter = new JFileChooser(downloadDir);
		// Sets file filter to txt files
		batchBIDGetter.setFileFilter(new FileNameExtensionFilter("BundleID list", "txt"));
		// Check if file has been selected
		if (batchBIDGetter.showOpenDialog(frmBundleidfetcher) == JFileChooser.APPROVE_OPTION) {
		    System.out.println("Selected file: " + batchBIDGetter.getSelectedFile().getAbsolutePath());
		    // Download from selected file
		    downloadBatchFromFile(batchBIDGetter.getSelectedFile());
		}
	}
	
	
	// Downloads all BundleID's from File
	private void downloadBatchFromFile(File file) {
		// Runs downloading as async
		CompletableFuture.runAsync(() -> {
			// Create a Buffered Reader of the file
			try(BufferedReader br = new BufferedReader(new FileReader(file))) {
				// Loop through each line
			    for(String line; (line = br.readLine()) != null; ) {
			    	// If line is black then skip line
			    	if(line.isBlank())continue;
			    	// Append BundleID with Apple's API
		    		String url = "http:// itunes.apple.com/lookup?bundleId=" + line;
		    		// Get "results" JSONArray from the returned JSONObject
		    		JSONArray imageJSON = getJSONFromURL(url).getJSONArray("results");
		    		// Check if JSONArray is null and has children
		    		if(imageJSON != null && imageJSON.length() != 0) {
		    			try {
		    				// Download image from line
		    				downloadImage(imageJSON.getJSONObject(0).getString("artworkUrl512"), line, false);
		    				// Increase download count
		    				doneCount++;
		    			} catch (Exception e) {
		    				// Prints error if caught
		    				e.printStackTrace();
		    				// Increase missed count
		    				missedCount++;
		    				// Add missed BID to list
		    				missedBIDs.add(line);
		    			}
		    		} else {
	    				// Increase missed count
	    				missedCount++;
	    				// Add missed BID to list
	    				missedBIDs.add(line);
		    		}
			    }
			} catch (IOException e) {
				// Shows message box if cannot parse file
				msgBox("Error parsing " + file.getName());
			} finally {
				// Shows message box with amount of files downloaded and missed.
				msgBox(doneCount + " downloaded successfuly.\n" + missedCount + " failed to download.");
			}
		});
	}
	
	
	// Displays message box with message
	private void msgBox(String msg) {
		// Create new JOptionPane with message
		JOptionPane optionPane = new JOptionPane(msg ,JOptionPane.INFORMATION_MESSAGE);
		// Creates dialog with title
		JDialog dialog = optionPane.createDialog("BundleIDFetcher");
		// Set JOptionPane to always be on top
		dialog.setAlwaysOnTop(true);
		// Set JOptionPane to always be visible
		dialog.setVisible(true);
	}
	

	  /////////////////////////////////////////////////////////////////////// 
	 //                      HANDLED BY WINDOWBUILDER                     // 
	/////////////////////////////////////////////////////////////////////// 
	private void initialize() {
		
		frmBundleidfetcher = new JFrame();
		frmBundleidfetcher.setTitle("BundleIDFetcher");
		frmBundleidfetcher.setBounds(100, 100, 250, 300);
		frmBundleidfetcher.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		pnlHeader = new JPanel();
		FlowLayout flowLayout = (FlowLayout) pnlHeader.getLayout();
		flowLayout.setHgap(10);
		flowLayout.setVgap(10);
		frmBundleidfetcher.getContentPane().add(pnlHeader, BorderLayout.NORTH);
		
		lblHeader = new JLabel("BundleIDFetcher");
		lblHeader.setHorizontalAlignment(SwingConstants.CENTER);
		lblHeader.setFont(new Font("AppleGothic", Font.PLAIN, 26));
		pnlHeader.add(lblHeader);
		
		pnlContent = new JPanel();
		frmBundleidfetcher.getContentPane().add(pnlContent, BorderLayout.CENTER);
		pnlContent.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
		
		pnlContentHeader = new JPanel();
		pnlContent.add(pnlContentHeader);
		pnlContentHeader.setLayout(new FlowLayout(FlowLayout.LEADING, 5, 5));
		pnlContentHeader.setSize(pnlContentHeader.getParent().getSize().width - 10, pnlContentHeader.getSize().height);
		
		labelContentHeader = new JLabel("Enter BundleID");
		pnlContentHeader.add(labelContentHeader);
		labelContentHeader.setHorizontalAlignment(SwingConstants.LEFT);
		labelContentHeader.setFont(new Font("AppleGothic", Font.PLAIN, 18));
		
		txtBID = new JTextField();
		pnlContent.add(txtBID);
		txtBID.setHorizontalAlignment(SwingConstants.LEFT);
		txtBID.setColumns(18);
		
		btnGetImage = new JButton("Get BundleID Image");
		pnlContent.add(btnGetImage);
		btnGetImage.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
            	getImage();
            }
        });
		
		btnBatchGet = new JButton("Select file for Batch-Get");
		pnlContent.add(btnBatchGet);
		btnBatchGet.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
            	getBatch();
            }
        });
		
		pnlFooter = new JPanel();
		frmBundleidfetcher.getContentPane().add(pnlFooter, BorderLayout.SOUTH);
		pnlFooter.setLayout(new FlowLayout(FlowLayout.RIGHT, 10, 5));
		lblFooter = new JLabel("Made with ❤ in Germany and New Zealand");
		lblFooter.setHorizontalAlignment(SwingConstants.TRAILING);
		lblFooter.setFont(new Font("AppleGothic", Font.PLAIN, 10));
		pnlFooter.add(lblFooter);
	}

}
