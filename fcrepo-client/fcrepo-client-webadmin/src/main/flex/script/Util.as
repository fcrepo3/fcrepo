package script
{  
    public class Util
    {
        /**
         * Replaces all of the occurances of a substring within a string with a replacement value.
         * Cannot handle the case where the replacement value contains the string to replace.
         */ 
        public static function replaceAll(text:String, toReplace:String, replaceVal:String):String {
            while(text.indexOf(toReplace) > 0) {
                text = text.replace(toReplace, replaceVal);
            }
            return text;
        }
        
        /**
         * Performs URL encoding on a string
         */ 
        public static function urlEncode(text:String):String {
            var encoded:String = escape(text);
            encoded = replaceAll(encoded, "+", "%2B");
            return encoded;
        }
    }
}