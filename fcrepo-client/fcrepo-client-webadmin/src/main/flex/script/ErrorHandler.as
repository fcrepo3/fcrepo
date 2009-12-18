package script
{
    import flash.events.Event;
    import flash.events.IOErrorEvent;
    import flash.net.URLLoader;
    import flash.net.URLRequest;       
           
    public class ErrorHandler
    {
        public static var baseUrl:String;
        
        /**
         * Checks the returned value of an http request to determine
         * if an error occurred. Due to a flex/flash limitation errors
         * sent via the normal http fashion cannot be read, so by
         * asking the server to return with only 2xx range responses
         * and checking those responses for indications of an error
         * the error messages can be made available.
         */
        public static function getError(toCheck:Object):String {
           var toCheckText:String = toCheck.toString();
           var error:String = null;
           var errorIndicator:int = toCheckText.indexOf("::ERROR");
           if(errorIndicator >= 0) {
               var errorCode:String = 
                   toCheckText.substring(errorIndicator+8, errorIndicator+11);
               error = "Error (" + errorCode +"): " + 
                       toCheckText.substring(0, errorIndicator);
               if(error.length > 500) {
                   error = error.substring(0, 500) + "...";
               }
           }           
           return error;
        }
        
        /**
         * Backup error handler
         */ 
        public static function handleFault(event:Event):void { 
            // Check to see if the repository is still available           
            var repoCheck:URLLoader = new URLLoader();
            repoCheck.addEventListener(IOErrorEvent.IO_ERROR, 
                                       repoCheckFaultHandler);
            repoCheck.addEventListener(Event.COMPLETE, 
                                       function():void{displayError(event.toString());});            
            var repoRequest:URLRequest =
                    new URLRequest(baseUrl+"/objects");           
                    repoCheck.load(repoRequest);            
        }
        
        private static function repoCheckFaultHandler(event:Event):void {
            Alerts.showError("Your request failed to complete. The Fedora repository " + 
                             "does not appear to be responding. Please check that the " + 
                             "repository is available and attempt your request again.");  
        }
        
        private static function displayError(error:String):void {
            Alerts.showError("Error: " + error);
        }     
    }
}