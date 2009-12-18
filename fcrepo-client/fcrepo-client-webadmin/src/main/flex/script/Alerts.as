package script
{
    import mx.controls.Alert;
    import flash.display.Sprite;
    
    public class Alerts
    {
        public static function showInfo(text:String):void {
            var alert:Alert = Alert.show(text);
            alert.styleName = "info";
        }

        public static function showWarning(text:String):void {
            var alert:Alert = Alert.show(text);
            alert.styleName = "warning";
            alert.title = "Warning";
        }
                
        public static function showError(text:String):void {
            var alert:Alert = Alert.show(text);
            alert.styleName = "error";
            alert.title = "Error";
        }
        
        public static function showVerification(text:String, 
                                                title:String, 
                                                parent:Sprite, 
                                                handler:Function):void {
            var alert:Alert = Alert.show(text, title, 3, parent, handler);
            alert.styleName = "warning";        
        }        
    }
}