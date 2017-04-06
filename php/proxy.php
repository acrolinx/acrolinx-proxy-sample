<?php
/*
 * Acrolinx Proxy
 * Description - Creates proxy for Acrolinx.
 * Author - Acrolinx
 * Version - 1.0
 */
 
class Proxy {
    private $settings;
    private $targetPath;
    private $username;
    private $password;
    private $domainURL;	
	
    private function disable_gzip() {
        @ini_set('zlib.output_compression', 'Off');
        @ini_set('output_handler', '');	
    }
    
    
    private function getSettings() {
        $acrolinxSettings = array();

        $acrolinxSettings['isSSO'] = 1; //TODO: Set if SSO should be used, otherwise it will be just a reverse proxy.
        $acrolinxSettings['url'] = ""; //TODO: Set Acrolinx server url here.
        $acrolinxSettings['genericPassword'] = "secret"; //TODO: Set secret token here.

        if($acrolinxSettings['isSSO'] == 1) {

            $acrolinxSettings['password'] = $acrolinxSettings['genericPassword'];
            $acrolinxSettings['username'] = get_current_user(); //TODO: Implement get_current_user, which retrieves the username from the current session.
        }
        return $acrolinxSettings;
    }
    
    private function getPosition($requestURI, $part) {
        return strpos($requestURI, $part);
    }
    
    private function getActualURI($requestURI, $pos, $part) {
        return substr_replace($requestURI, '', 0, ($pos+strlen($part)));
    }
    
    private function getHeadersOfRequest() {
        $headers = array();
        $isAuthToken = false; 
        foreach (getallheaders() as $name => $value) {
          $headerString = $name.':'.$value;

          //Setting target Server as Host
          if(strtolower($name) == 'host') {
              $headerString = $name.':'.parse_url(self::getSettings()['url'])['host'];
          }

          if($name == 'authToken') {
            $isAuthToken = true;
          }
          array_push($headers,"$headerString");
        }
        
        if($isAuthToken == false){
            $settings = self::getSettings();
            $username = 'username:'.$settings['username'];
            $password = 'password:'.$settings['password'];
            array_push($headers, $username);
            array_push($headers, $password);
            array_push($headers,'User-Agent:Tinymce Proxy');
        }
        return $headers;
    }
    
    private function getPOSTdata(){
        return file_get_contents("php://input");
    }
    
    private function proxyRequest($url){
        $ch = curl_init();
        curl_setopt ($ch, CURLOPT_URL, $url);
//To write all requests to std error, uncomment the following line of code.
//By default, Apache will write to /var/log/apache2/error.log.
//Don’t use this sample in production. All passwords, cookies, and tokens are logged.
//	    curl_setopt ($ch, CURLOPT_VERBOSE, true); 
        
        if($_SERVER['REQUEST_METHOD'] == 'POST'){
            $postData = self::getPOSTdata();  
            curl_setopt ($ch, CURLOPT_POST, 1);
            curl_setopt ($ch, CURLOPT_POSTFIELDS, $postData);
        }
        
        if($_SERVER['REQUEST_METHOD'] == 'PUT'){
            curl_setopt($ch, CURLOPT_PUT, 1);
        }
        
        $headers = self::getHeadersOfRequest(); 
     
        curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);
        curl_setopt($ch, CURLOPT_HEADER, 1);
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, 1);
								
        $response = curl_exec ($ch);
        if (curl_error($ch)){
            print curl_error($ch);
        } else {
            self::processResponse($response);
        }
        curl_close ($ch);
    }
    
    private function processResponse($response) {
        $response = str_replace("HTTP/1.1 100 Continue\r\n\r\n","",$response);
        $responseArray = explode("\r\n\r\n", $response, 2);
        $headers = $responseArray[0];
        $result = $responseArray[1];
        $headerArray = split(chr(10), $headers); 
        
        foreach($headerArray as $header=>$headerValue){
            if(!preg_match("/^Transfer-Encoding/", $headerValue)){
                $headerValue = str_replace($targetPath, $domainURL, $headerValue);
                header(trim($headerValue));
            }
        }
        
        if(strpos($requestURI,'report.xml') !== false){
            echo $result;
        } else {
            $result = str_replace($targetPath, $domainURL, $result);
            print $result;
        }
    }
    
    public function initialize() {
        self::disable_gzip();
        $settings = self::getSettings();
        $targetPath = $settings['url'];
        if($_SERVER['HTTPS'] == 'on'){
            $domainURL = 'https://'.$_SERVER['HTTP_HOST'];
        } else {
            $domainURL = 'http://'.$_SERVER['HTTP_HOST'];
        }
        $part = '/acrolinx/proxy.php'; //TODO: Ensure that this is the correct location of the 'proxy.php' on the server.
        $requestURI = $_SERVER['REQUEST_URI'];
        $pos = self::getPosition($requestURI, $part);
        if ($pos !== false) {
            $requiredURI = self::getActualURI($requestURI, $pos, $part);
            $url = $targetPath.$requiredURI;
        }
        self::proxyRequest($url); 
    }
}
Proxy::initialize();
?>