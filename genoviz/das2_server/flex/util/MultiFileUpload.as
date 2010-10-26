///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
//	Multi-File Upload Component Ver 1.1
//
//  Copyright (C) 2006 Ryan Favro and New Media Team Inc.
//  This program is free software; you can redistribute it and/or
//  modify it under the terms of the GNU General Public License
//  as published by the Free Software Foundation; either version 2
//	of the License, or (at your option) any later version.
//	
//	This program is distributed in the hope that it will be useful,
//	but WITHOUT ANY WARRANTY; without even the implied warranty of
//	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//	GNU General Public License for more details.
//	
//	You should have received a copy of the GNU General Public License
//	along with this program; if not, write to the Free Software
//	Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
//
//	Any questions about this component can be directed to it's author Ryan Favro at ryanfavro@hotmail.com
//
//  To use this component create a new instance of this component and give it ten parameters
//
//	EXAMPLE:
//
//	multiFileUpload = new MultiFileUpload(
//	      filesDG,   		// <-- DataGrid component to show the cue'd files
//	      browseBTN, 		// <-- Button componenent to be the browser button
//	      clearButton, 		// <-- Button component to be the button that removes all files from the cue
//	      delButton, 		// < -- Button component to be the button that removes a single selected file from the cue
//	      upload_btn, 		// <-- Button component to be the  button that triggers the actual file upload action
//	      progressbar, 		// <-- ProgressBar Component that will show the file upload progress in bytes
//	      "http://[Your Server Here]/MultiFileUpload/upload.cfm", // <-- String Type the url to the server side upload component can be a full domain or relative
//	      postVariables,  	// < -- URLVariables type that will contain addition variables to accompany the upload
//	      350000, 			//< -- Number type to set the max file size for uploaded files in bytes. A value of 0 (zero) = no file limit
//	      filesToFilter 	// < -- Array containing FileFilters an empty Array will allow all file types
//	       );
//
//
//
//	Enjoy!
//
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////



package util 
{


	// Imported Class Definitions
    import flash.events.*;
    import flash.net.FileReference;
    import flash.net.FileReferenceList;
    import flash.net.URLRequest;
    import flash.net.URLVariables;
    
    import mx.collections.ArrayCollection;
    import mx.controls.Alert;
    import mx.controls.Button;
    import mx.controls.DataGrid;
    import mx.controls.ProgressBar;
    import mx.controls.dataGridClasses.*;
    import mx.events.CloseEvent;
    import mx.events.CollectionEvent;
    
    
    public class MultiFileUpload {
    
    	
    	
        //UI Vars
        private var _datagrid:DataGrid;
        private var _browsebutton:Button;
        private var _remselbutton:Button;
        private var _remallbutton:Button;
        private var _uploadbutton:Button;
        private var _progressbar:ProgressBar;
        private var _testButton:Button;

        //DataGrid Columns
        private var _nameColumn:DataGridColumn;
        private var _typeColumn:DataGridColumn;
        private var _sizeColumn:DataGridColumn;
        private var _creationDate:DataGridColumn;
        private var _modificationDate:DataGridColumn;
        private var _progressColumn:DataGridColumn;
        private var _columns:Array;
        
        //File Reference Vars
        [Bindable]
        private var _files:ArrayCollection;
        private var _fileref:FileReferenceList
        private var _file:FileReference;
        private var _uploadURL:URLRequest;
        private var  _totalbytes:Number;
        
        //File Filter vars
        private var _filefilter:Array;

        //config vars
        private var _url:String; // location of the file upload handler can be a relative path or FQDM
        private var _maxFileSize:Number; //bytes
        private var _variables:URLVariables; //variables to passed along to the file upload handler on the server.
        
        private var _skipCount:int = 0;
        
        //Constructor    
        public function MultiFileUpload(
        								dataGrid:DataGrid,
        								browseButton:Button,
        								removeAllButton:Button,
                                        removeSelectedButton:Button,
                                        uploadButton:Button,
                                        progressBar:ProgressBar,
                                        url:String,
                                        variables:URLVariables,
                                        maxFileSize:Number,
                                        filter:Array
                                        ){
            _datagrid = dataGrid;
            _browsebutton = browseButton;
            _remallbutton = removeAllButton;
            _remselbutton = removeSelectedButton;            
            _uploadbutton = uploadButton;
            _url = url;
            _progressbar = progressBar;
            _variables = variables;
            _maxFileSize = maxFileSize;
            _filefilter = filter;
            _skipCount = 0;
            init();
        }
        
        //Initialize  Component
        private function init():void{
	        
	        // Setup File Array Collection and FileReference
	        _files = new ArrayCollection();
	        _fileref = new FileReferenceList;
	        _file = new FileReference;
	        
	        // Set Up Total Byes Var
	        _totalbytes = 0;
	        
	        // Add Event Listeners to UI 
	        _browsebutton.addEventListener(MouseEvent.CLICK, browseFiles);
	        _uploadbutton.addEventListener(MouseEvent.CLICK,uploadFiles);
	        _remallbutton.addEventListener(MouseEvent.CLICK,clearFileCue);
	        _remselbutton.addEventListener(MouseEvent.CLICK,removeSelectedFileFromCue);
	        _fileref.addEventListener(Event.SELECT, selectHandler);
	        _fileref.addEventListener(DataEvent.UPLOAD_COMPLETE_DATA, dataCompleteHandler);
	        _files.addEventListener(CollectionEvent.COLLECTION_CHANGE,popDataGrid);
	        
	        
	        // Set Up Progress Bar UI
	        _progressbar.mode = "manual";
	        _progressbar.label = "";
	        
	        // Set Up UI Buttons;
	        //_uploadbutton.enabled = false;
	        _remselbutton.enabled = false;
	        _remallbutton.enabled = false;
	        
	        
	        // Set Up DataGrid UI
	        _nameColumn = new DataGridColumn;
	        _typeColumn = new DataGridColumn;
	        _sizeColumn = new DataGridColumn;
	            
	        _nameColumn.dataField = "name";
	        _nameColumn.headerText= "File";
	        
	        _typeColumn.dataField = "type";
	        _typeColumn.headerText = "File Type";
	        _typeColumn.width = 80;
	        
	        _sizeColumn.dataField = "size";
	        _sizeColumn.headerText = "File Size";
	        _sizeColumn.labelFunction = bytesToKilobytes as Function;
	        _sizeColumn.width = 150;
	        
	        _columns = new Array(_nameColumn,_typeColumn,_sizeColumn);
	        _datagrid.columns = _columns
	        _datagrid.sortableColumns = false;
	        _datagrid.dataProvider = _files;
	        _datagrid.dragEnabled = true;
	        _datagrid.dragMoveEnabled = true;
	        _datagrid.dropEnabled = true;
	    	
	    	// Set Up URLRequest
	        _uploadURL = new URLRequest;
	        _uploadURL.url = _url;
	        _uploadURL.method = "POST";  // this can also be set to "POST" depending on your needs 
	        
	        _uploadURL.data = _variables;
	        _uploadURL.contentType = "multipart/form-data";
	        
	        
        }
        
        /********************************************************
        *   PRIVATE METHODS                                     *
        ********************************************************/
		        
		

        
        //Browse for files
        private function browseFiles(event:Event):void{        
                
                _fileref.browse(_filefilter);
                
            }
            
        public function startUpload():void {
        	uploadFiles(null);
        }

		//Upload File Cue
        private function uploadFiles(event:Event):void{
           
            if (_files.length > 0){

                _file = FileReference(_files.getItemAt(0));    

				var viableFile:Boolean = true;
            	try{
					_file.size;
					if (_file.size == 0) {
						viableFile = false;
						trace("Skipping 0 length file " + _file.name);
						_progressbar.label = "Skipping 0 length file " + _file.name;
					}
				} catch(e:Error){
					viableFile = false;
					Alert.show("Skipping file " + _file.name + " " + e.message)
					_progressbar.label = "Skipping invalid file " + _file.name;
				}
				
				if (viableFile) {
		            _file.addEventListener(Event.OPEN, openHandler);
		            _file.addEventListener(ProgressEvent.PROGRESS, progressHandler);
		            _file.addEventListener(Event.COMPLETE, completeHandler);
		            _file.addEventListener(DataEvent.UPLOAD_COMPLETE_DATA, dataCompleteHandler );
		            _file.addEventListener(SecurityErrorEvent.SECURITY_ERROR,securityErrorHandler);
		            _file.addEventListener(HTTPStatusEvent.HTTP_STATUS,httpStatusHandler);
		            //_file.addEventListener(IOErrorEvent.IO_ERROR,ioErrorHandler);
		            _file.upload(_uploadURL);
		             setupCancelButton(true);
				} else {
					_skipCount++;
					_files.removeItemAt(0);
					uploadFiles(null);
					
					if (_files.length == 0) {
					     setupCancelButton(false);
                		 _progressbar.label = "Uploads Complete";
        		         var uploadCompleted:Event = new Event(Event.COMPLETE);
		                dispatchEvent(uploadCompleted);
					}

				}
            }
        }
        
        public function getSkipCount():int {
        	return _skipCount;
        }
        
        public function setUploadURLParameters(params:URLVariables):void {
        	this._uploadURL.data = params;
        }
        
        //Remove Selected File From Cue
        private function removeSelectedFileFromCue(event:Event):void{
           
            if (_datagrid.selectedIndex >= 0){
            _files.removeItemAt( _datagrid.selectedIndex);
            }
        }


		 //Remove all files from the upload cue;
        private function clearFileCue(event:Event):void{
       
            _files.removeAll();
        }
		
		// Cancel Current File Upload
        private function cancelFileIO(event:Event):void{
        	
            _file.cancel();
            setupCancelButton(false);
            checkCue();
            
        }    
    
       


		//label function for the datagird File Size Column
        private function bytesToKilobytes(data:Object,blank:Object):String {
            var kilobytes:String;
            kilobytes = String(Math.round(data.size/ 1024)) + ' kb';
            return kilobytes
        }
        
        
        // Feed the progress bar a meaningful label
        private function getByteCount():void{
	        var i:int;
	        _totalbytes = 0;
	            for(i=0;i < _files.length;i++){
	            _totalbytes +=  _files[i].size;
	            }
	        _progressbar.visible = true;
	        _progressbar.label = _files.length + " files, " + Math.round(_totalbytes/1024) + " kb total size"
        }        
        
        // Checks the files do not exceed maxFileSize | if _maxFileSize == 0 No File Limit Set
        private function checkFileSize(filesize:Number):Boolean{
      
        	var r:Boolean = false;
        		//if  filesize greater then _maxFileSize
		        if (filesize > _maxFileSize){
		        	r = false;
		       	 }else if (filesize <= _maxFileSize){
		        	r = true;
	        	}
	        	
	        	if (_maxFileSize == 0){
	        	r = true;
	        	}
	   	
        	return r;
        }
        
        // restores progress bar back to normal
        private function resetProgressBar():void{
        
                  _progressbar.label = "";
                  _progressbar.visible = false;
                 _progressbar.maximum = 0;
                 _progressbar.minimum = 0;
        }
        
        // reset form item elements
        private function resetForm():void{
            //_uploadbutton.enabled = false;
            _uploadbutton.addEventListener(MouseEvent.CLICK,uploadFiles);
            _uploadbutton.label = "Upload";
            _progressbar.maximum = 0;
            _totalbytes = 0;
            _progressbar.label = "";
            _remselbutton.enabled = false;
            _remallbutton.enabled = false;
            _browsebutton.enabled = true;
        }
        
        // whenever the _files arraycollection changes this function is called to make sure the datagrid data jives
        private function popDataGrid(event:CollectionEvent):void{                
            getByteCount();
            checkCue();
        }
        
       // enable or disable upload and remove controls based on files in the cue;        
        private function checkCue():void{
             if (_files.length > 0){
                //_uploadbutton.enabled = true;
                _remselbutton.enabled = true;
                _remallbutton.enabled = true;            
             }else{
                resetProgressBar();
                //_uploadbutton.enabled = false;     
             }    
        }

    	// toggle upload button label and function to trigger file uploading or upload cancelling
        private function setupCancelButton(x:Boolean):void{
            if (x == true){
                _uploadbutton.label = "Cancel Upload";
                _browsebutton.enabled = false;
                _remselbutton.enabled = false;
                _remallbutton.enabled = false;
                _uploadbutton.addEventListener(MouseEvent.CLICK,cancelFileIO);        
            }else if (x == false){
                _uploadbutton.removeEventListener(MouseEvent.CLICK,cancelFileIO);
                 resetForm();
            }
        }
        

       /*********************************************************
       *  File IO Event Handlers                                *
       *********************************************************/
      
        //  called after user selected files form the browse dialouge box.
        private function selectHandler(event:Event):void {
            var i:int;
            var msg:String ="";
            var dl:Array = [];                          
	            for (i=0;i < event.currentTarget.fileList.length; i ++){
	            	if (checkFileSize(event.currentTarget.fileList[i].size)){
	                	_files.addItem(event.currentTarget.fileList[i]);
	                	var browseFileEvent:Event = new Event(Event.OPEN);
			            dispatchEvent(browseFileEvent);

	            	}  else {
	            	dl.push(event.currentTarget.fileList[i]);
	            	}
	            }	            
	            if (dl.length > 0){
	            	for (i=0;i<dl.length;i++){
	            	msg += String(dl[i].name + " is too large. \n");
	            	}
	            	mx.controls.Alert.show(msg + "Max File Size is: " + Math.round(_maxFileSize / 1024) + " kb","File Too Large",4,null).clipContent;
	            }        
        }        
        
        // called after the file is opened before upload    
        private function openHandler(event:Event):void{
            _files;
        }
        
        // called during the file upload of each file being uploaded | we use this to feed the progress bar its data
        private function progressHandler(event:ProgressEvent):void {
        	_progressbar.visible = true;        
            _progressbar.setProgress(event.bytesLoaded,event.bytesTotal);
            _progressbar.label = "Uploading " + Math.round(event.bytesLoaded / 1024) + " kb of " + Math.round(event.bytesTotal / 1024) + " kb (" + (_files.length - 1) + " files remaining)";
        }

		private function dataCompleteHandler(dataCompleteEvent:DataEvent) :void
		{
			dispatchEvent(dataCompleteEvent);
            _files.removeItemAt(0);
            if (_files.length > 0){
            	_totalbytes = 0;
                uploadFiles(null);
            }else{
                setupCancelButton(false);
                 _progressbar.label = "Uploads Complete";
                 var uploadCompleted:Event = new Event(Event.COMPLETE);
                dispatchEvent(uploadCompleted);
            }
		} 

        // called after a file has been successully uploaded | we use this as well to check if there are any files left to upload and how to handle it
        private function completeHandler(event:Event):void{
        }    
          
        // only called if there is an  error detected by flash player browsing or uploading a file   
        private function ioErrorHandler(event:IOErrorEvent):void{
            mx.controls.Alert.show("File '" + event.target.name + "' did not upload.\n" + event.text,
            						"Upload IO error",0);
        }    
        // only called if a security error detected by flash player such as a sandbox violation
        private function securityErrorHandler(event:SecurityErrorEvent):void{
            mx.controls.Alert.show(String(event),"Security Error",0);
        }
        
        //  This function its not required
        private function cancelHandler(event:Event):void{
            // cancel button has been clicked;
        }
        
        //  after a file upload is complete or attemted the server will return an http status code, code 200 means all is good anything else is bad.
        private function httpStatusHandler(event:HTTPStatusEvent):void {
            if (event.status != 200){
            	var message:String = "";
            	var title:String = "";
            	if (event.status == 902) {
            		message = "File '" + event.target.name + "' did not upload.\nUnsupported file extension.";
            		title = "Unsupported file extension";
            	} else if (event.status == 903) {
            		message = "File '" + event.target.name + "' did not upload.\nThe file name does not match any of the segment names for the genome version.";
            		title =  "Incorrect File Name";
            		
            	} else if (event.status == 905) {
            		message = "File '" + event.target.name + "' did not upload.\nThe text formatted file exceeds the maximum allowed size (10,000 lines).\nConvert to xxx.useq\n(see http://useq.sourceforge.net/useqArchiveFormat.html) or other binary form.";
            		title =  "Text File exceeds max allowed size";            		
            	} else {
            		message = "File '" + event.target.name + "' did not upload.\nStatus code " + event.status + " returned.";
            		title = "Upload Error";
            	}
            	
            	if (_files.length > 1) {
            		mx.controls.Alert.show(message + "\nDo you want to proceed with the remaining uploads?",
            						title,
            						(Alert.YES | Alert.NO), 
            						null,
            						onPromptToContinueUpload);
            	} else {
            		mx.controls.Alert.show(message,
            						title);
            	}
            	
            }
        }
        
       private function onPromptToContinueUpload(event:CloseEvent):void {
			if (event.detail == Alert.YES) {
	            _files.removeItemAt(0);
	            if (_files.length > 0){
	            	_totalbytes = 0;
	                uploadFiles(null);
	            }else{
	                setupCancelButton(false);
	                 _progressbar.label = "Uploads Complete";
	                 var uploadCompleted:Event = new Event(Event.COMPLETE);
	                dispatchEvent(uploadCompleted);
	            }
   			}else{
	            setupCancelButton(false);
	            _progressbar.label = "Uploads Cancelled";
	            var completeEvent:Event = new Event(Event.COMPLETE);
	            dispatchEvent(completeEvent);
	        }
        }

        
    }
}