/*
Copyright (c) 2011-2013, Lorenzo Gomez (lorenzobgomez@gmail.com)
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, 
are permitted provided that the following conditions are met:

Redistributions of source code must retain the above copyright notice, this list of 
conditions and the following disclaimer.

Redistributions in binary form must reproduce the above copyright notice, this list 
of conditions and the following disclaimer in the documentation and/or other materials 
provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY 
EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES 
OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT 
SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, 
INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED 
TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR 
BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN 
ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;


public class Translate
{
	private static Process procReplay;
	
	public static void main(String[] args) 
	{
		//ARG[0] = location of getevent log
		//ARG[1] = dir of output of sendEvents.txt
		//ARG[2-3] = flag -e is the valid event list, if none specified, then do them all
		//		     flag -t are the time-warp values
		
		String line = "";
		String inputDeviceType = "";
		long type = 0;
		long code = 0;
		long value = 0;
		double prevTimestamp = 0.0;
		int sameCounter = 0;
		boolean allEventsValid = false;
		
		File wd = new File("/bin");
				
		try 
		{
			procReplay = Runtime.getRuntime().exec("/bin/bash", null, wd);
		}
		catch (IOException e) {e.printStackTrace();}
		
		if(procReplay != null) 
		{
			String eventsFilePath = args[0];
			String splitEventOn = ",";
			String[] validEventNumbers = null;
			
			if(args.length >= 3)
			{
				int parameterIndexE = parameterCheck(args, "-e");
				if(parameterIndexE > 0)							
					validEventNumbers = args[parameterIndexE].split(splitEventOn);
				else
					allEventsValid = true;
			}
			else
				allEventsValid = true;
			
			// "numberOfLines" is the number of events that will be sent into the replay program
			// on the phone. Note, this will not include ignored events (user specified)			
			int numberOfLines = 0;			
			BufferedReader reader;
			try 
			{
				reader = new BufferedReader(new FileReader(eventsFilePath));
		
				String temp = "";
				
				// Ignore the initial event mapping text
				// and ignore text that shows the device has started
				while ((temp = reader.readLine()) != null)
				{
					if((temp.lastIndexOf("event") != -1) && (temp.lastIndexOf("device") == -1) && (temp.lastIndexOf("name") == -1)) 					
					{	
						String[] tokens = null;
						String splitOn = " ";
											
						tokens = temp.split(splitOn);					
							
						inputDeviceType = tokens[1];
						inputDeviceType = removeColon(inputDeviceType);
						String eventNumber = getInputDevice(inputDeviceType);
						int currentEventIsValid = 0;
						
						// Check what eventNumber's are valid
						// Given as input arg2, ex. 3,4,5,6
						// For the valid events, continue to build the timestamp and write the event
						if(!allEventsValid)
						{
							for(int x = 0; x<validEventNumbers.length; x++)
							{								 
								if(eventNumber.equals(validEventNumbers[x]))
									currentEventIsValid = 1;
							}
							if(currentEventIsValid == 1)
								numberOfLines++;
						}
						else
						{
							numberOfLines++;
						}
						
					}					
				}
				reader.close();
			} catch (Exception e) {e.printStackTrace();}
			
			// Read the getevent file and build the event file
			File eventsFile = new File(eventsFilePath);
			InputStream inStream = null;
			try{
				inStream = new FileInputStream(eventsFile);
			} catch (FileNotFoundException e2) {e2.printStackTrace();}
			DataInputStream inDataStream = new DataInputStream(inStream);
			BufferedReader fileReader = new BufferedReader(new InputStreamReader(inDataStream));			
						
			FileOutputStream outputStream;			
			PrintStream output = null;
			try{
				outputStream = new FileOutputStream(args[1]);
				output = new PrintStream(outputStream);
			}catch(IOException e3) {e3.printStackTrace();}			
			
			// Print the number of lines to be processed, this will determine the size
			// of the buffer for the replay agent
			boolean isFirstEvent = true;			
			output.println(numberOfLines);			
			int written = 0;
								
			// For each line, check to see if the user wants the event type,
			// build the type, code, value, and then write it to output			
			try{//needed for fileReader.readLine()
				while ((line = fileReader.readLine()) != null) //while(STREAM ! NULL)
				{				
					String[] tokens = null;
					String timestampString = "";					
					double timestamp = 0.0;					
					long interval = 0;
					String splitOn = " ";
					int seconds = 0;
					int microseconds = 0;
					
					line = line.replaceAll("[\\[][ ]*", "").replace("]", "");
					tokens = line.split(splitOn);					
					timestampString = tokens[0];
					
					if(!timestampString.equals("add") && timestampString.length() != 0 && !timestampString.equals("could"))
					{										
						timestampString = removeColon(timestampString);
						timestampString = timestampString.replace(".", "-");

						String[] times = null;											
						times = timestampString.split("-");
						seconds = stringToInt(times[0]);
						microseconds = stringToInt(times[1]);
						timestamp = seconds + ((double)microseconds/1000000);
						
						inputDeviceType = tokens[1];
						inputDeviceType = removeColon(inputDeviceType);						
						String eventNumber = getInputDevice(inputDeviceType);
						
												
						// Check what eventNumber's are valid
						// Given as input arg, ex. 3,4,5,6
						// For the valid events, continue to build the timestamp and write the event						
						boolean currentEventIsValid = false;
						
						if(!allEventsValid)
						{
							for(int x = 0; x<validEventNumbers.length; x++)
							{								 
								if(eventNumber.equals(validEventNumbers[x]))
									currentEventIsValid = true;
							}
						}
						
						if((currentEventIsValid && !eventNumber.equals("*")) || (allEventsValid && !eventNumber.equals("*")))//7-18-12
						{
							type = hexToLong(tokens[2]);
							code = hexToLong(tokens[3]);
							value = hexToLong(tokens[4]);
		
							// Check to see if this is the first event,
							// if it is then we want the sendevent to start right away
							// so make the interval 0
							if(isFirstEvent)
							{
								prevTimestamp = timestamp;
							}						
							
							// Calculate the interval by looking at the timestamps
							long longTimestamp = (long)(timestamp * 1000000000);
							long longPrevTimestamp = (long)(prevTimestamp * 1000000000);
							interval = longTimestamp - longPrevTimestamp;					
							
							if(interval >= 0)
							{								
								long intervalNano = interval;
																							
								if(args.length >= 3)//check if time-warping arguments were given 
								{
									int parameterIndexT = parameterCheck(args, "-t");
									if(parameterIndexT > 0)
									{
										// Time warping
										// arg given as MIN,LOW,NEW,MAX
										// all given in nanosec
										// MIN - minimum value (any interval less will run normally, usually gestures)
										// LOW - value detecting data entry
										// NEW - new value to replace the slow data entry 
										// MAX - maximum value to wait before cutting off
										// Ex]
										// if(.09sec < interval < .7sec) => change interval to .001sec
										// else if(interval > 3sec) => change interval to 3sec
										// otherwise => leave interval unchanged (gestures and intervals under 3sec)
										
										String[] timeWarpingValues = null;								
										timeWarpingValues = args[parameterIndexT].split(",");
										long min = (long)(1000000000l * Float.parseFloat(timeWarpingValues[0]));
										long low = (long)(1000000000l * Float.parseFloat(timeWarpingValues[1]));
										long newValue = (long)(1000000000l * Float.parseFloat(timeWarpingValues[2]));
										long max = (long)(1000000000l * Float.parseFloat(timeWarpingValues[3]));
										
										if(!(min < low && min < max && low < max))
											throw new IOException("ERROR: Bad range of time-warp values, check documentation.");
										
										if((intervalNano > min)
												&& (intervalNano < low))										
										{									
											intervalNano = newValue;
										}
										else if(intervalNano > max)
										{
											intervalNano = max;
										}
									}
								}
								
								try{
										
									if(intervalNano == 0)
									{
										sameCounter++;									
									}
									// ** Write the time interval (time to sleep) **
									output.println(intervalNano);								
								}catch(Exception e){e.printStackTrace();}				
							}
							else
							{
								System.out.println("ERROR, time interval between events should not be negative! Please check getevent log for event types that can be ignored.");
								break;
							}
							
							// ** Write the event details to output **
							output.println(eventNumber + "," + type + "," + code + "," + value);
							
							written++;							
							prevTimestamp = timestamp;							
							
							if(isFirstEvent)
							{							
								isFirstEvent = false;
							}
							
						}
						else
						{
							// Ignore the lines that start with * (usually when the device is started)
							// And ignore lines that the user chooses to skip, and continue to the next 
						}
					}
					else
					{
						// Ignore these lines because they are the initial output of "add device" and "name" of getevent
					}				
				}//end of while controlling the sending events from the log\
				
			} catch (IOException e1) {e1.printStackTrace();}
			
			System.out.println("Total number of events written is" + written);
			
			try
			{
				inStream.close();
				inDataStream.close();
				fileReader.close();				
			}catch (Exception e) {e.printStackTrace(); System.out.println("Error!");}
				
		}//end of if process was Good
	}	
	
	static long hexToLong(String hex)
	{
		return(Long.parseLong(hex, 16));		
	}
	
	static String getInputDevice(String path)
	{
		return(path.replaceAll("/dev/input/event", ""));		
	}
	
	static String removeColon(String original)
	{
		return(original.replaceAll(":", ""));
	}
	
	static int stringToInt(String s)
	{
		return(Integer.parseInt(s));
	}
	
	static long stringToLong(String s)
	{
		return(Long.parseLong(s));
	}
	
	static double timestampToSeconds(String timestamp)
	{
		timestamp = timestamp.replaceAll("-", ".");
		return (Double.parseDouble(timestamp));		
	}
	
	static int parameterCheck(String[] args, String flag)
	{
		int i;
		boolean found = false;
		
		for(i = 0; i < args.length; i++)
		{
			if(args[i].equals(flag))
			{
				found = true;
				break;
			}
		}
		if(!found)
			i=-2;
		return i+1;
	}
	
}


