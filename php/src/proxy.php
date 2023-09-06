<?php
/*
 * Acrolinx Proxy
 * Description - Creates proxy for Acrolinx.
 * Author - Acrolinx
 * Version - 1.1
 */
class Proxy
{
    private $settings;
    private $targetPath;
    private $username;
    private $password;
    private $domainURL;
    private $host;
    private $url;

    public function __construct()
    {
        //If response is getting truncated try disabling gzip
        //self::disable_gzip();

        $this->settings = self::getSettings();
        $this->targetPath = rtrim($this->settings['url'], "/");
        $this->host = parse_url($this->targetPath)['host'];

        if (isset($_SERVER['HTTPS']) && $_SERVER['HTTPS'] == 'on') {
            $this->domainURL = 'https://' . $_SERVER['HTTP_HOST'];
        } else {
            $this->domainURL = 'http://' . $_SERVER['HTTP_HOST'];
        }

        $part = basename(__FILE__);
        $requestURI = $_SERVER['REQUEST_URI'];
        $pos = self::getPosition($requestURI, $part);

        if ($pos !== false) {
            $requiredURI = self::getActualURI($requestURI, $pos, $part);
            $this->url = $this->targetPath . $requiredURI;
        } else {
            echo "Ensure correct location of proxy.php on server is set. Check $part variable.";
        }
    }

    private function getSettings()
    {
        $acrolinxSettings = array();
        $acrolinxSettings['isSSO'] = 1; //TODO: Set if SSO should be used, otherwise it will be just a reverse proxy.
        $acrolinxSettings['url'] = ""; //TODO: Set Acrolinx server url here.
        $acrolinxSettings['genericPassword'] = "secret"; //TODO: Set secret token here.

        //TODO: Make sure not to call the following line in case a user is not authenticated to the application.
        if ($acrolinxSettings['isSSO'] == 1) {
            $acrolinxSettings['password'] = $acrolinxSettings['genericPassword'];
            $acrolinxSettings['username'] = get_current_user(); //TODO: Implement get_current_user, which retrieves the username from the current session.
        }

        return $acrolinxSettings;
    }

    private function getPosition($requestURI, $part)
    {
        return strpos($requestURI, $part);
    }

    private function getActualURI($requestURI, $pos, $part)
    {
        return substr_replace($requestURI, '', 0, ($pos + strlen($part)));
    }

    // This function is used as a fallback for getallheaders on Nginx server.

    public function processRequest()
    {
        $ch = curl_init();
        curl_setopt($ch, CURLOPT_URL, $this->url);

        //To write all requests to std error, uncomment the following line of code.
        //By default, Apache will write to /var/log/apache2/error.log.
        //Don't use this sample in production. All passwords, cookies, and tokens are logged.
        //curl_setopt ($ch, CURLOPT_VERBOSE, true);

        if ($_SERVER['REQUEST_METHOD'] == 'POST') {
            $postData = self::getPOSTdata();
            curl_setopt($ch, CURLOPT_POST, 1);
            curl_setopt($ch, CURLOPT_POSTFIELDS, $postData);

        } else if ($_SERVER['REQUEST_METHOD'] == 'PUT') {
            $postData = self::getPOSTdata();
            curl_setopt($ch, CURLOPT_HTTPHEADER, array('Content-Type: ' . $_SERVER["CONTENT_TYPE"] . ''));
            curl_setopt($ch, CURLOPT_CUSTOMREQUEST, 'PUT');
            curl_setopt($ch, CURLOPT_POSTFIELDS, $postData);

        } else if ($_SERVER['REQUEST_METHOD'] == 'DELETE') {
            $postData = self::getPOSTdata();
            curl_setopt($ch, CURLOPT_CUSTOMREQUEST, 'DELETE');
            curl_setopt($ch, CURLOPT_POSTFIELDS, $postData);
        } else if (!($_SERVER['REQUEST_METHOD'] == 'GET')) {
            header("HTTP/1.0 405 Method Not Allowed");
            exit();
        }

        $headers = self::getHeadersOfRequest();

        curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);
        curl_setopt($ch, CURLOPT_HEADER, 1);
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, 1);
        curl_setopt($ch, CURLOPT_FOLLOWLOCATION, 0);

        //Uncomment below 2 lines to disable SSL verification

        //curl_setopt($ch, CURLOPT_SSL_VERIFYHOST, 0);
        //curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, 0);

        //Uncomment and set certificate path for SSL verification.

        //curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, true);
        //curl_setopt($ch, CURLOPT_SSL_VERIFYHOST, 2);
        //curl_setopt($ch, CURLOPT_CAINFO, getcwd() . "path to .crt file");

        $response = curl_exec($ch);

        if (curl_error($ch)) {
            print curl_error($ch);
        } else {
            self::processResponse($response);
        }

        curl_close($ch);
    }

    private function getPOSTdata()
    {
        return file_get_contents("php://input");
    }

    private function getHeadersOfRequest()
    {
        $headers = array();
        $isAuthToken = false;
        $requestHeaders = (function_exists('getallheaders')) ? getallheaders() : self::emulate_getallheaders();
        $headers = array();
        $isHostHeaderPresent = 0;

        foreach ($requestHeaders as $name => $value) {
            $headerString = $name . ':' . $value;

            //Setting target Server as Host
            if (strtolower($name) == 'host') {
                $headerString = $name . ':' . $this->host;
                $isHostHeaderPresent = 1;
            }
            //Remove cookies from request
            if (strtolower($name) == 'cookie') {
                $acrolincCookies = self::filterCookies($value);
                array_push($headers, $name . ": " . $acrolincCookies);
            }
            array_push($headers, "$headerString");
        }

        $this->username = 'username:' . $this->settings['username'];
        $this->password = 'password:' . $this->settings['password'];

        if (!$isHostHeaderPresent) {
            array_push($headers, 'Host:' . $this->host);
        }

        array_push($headers, $this->username);
        array_push($headers, $this->password);
        array_push($headers, 'User-Agent:Acrolinx Proxy');
        array_push($headers,'X-Acrolinx-Integration-Proxy-Version:2');
        array_push($headers,'X-Acrolinx-Base-Url:' . $this->constructBaseUrl());

        return $headers;
    }

    private function filterCookies($cookieString) {
        $acrolinxCookies = array();
        $cookies =  explode(";", $cookieString);

        foreach ($cookies as &$cookie) {
            $trimedCookie = trim($cookie, " ");
            if (strpos($trimedCookie, 'X-Acrolinx-') === 0) {
                array_push($acrolinxCookies, $trimedCookie);
            }
        }

        return join(";", $acrolinxCookies);
    }

    private function constructBaseUrl(){
        $baseURL =  sprintf(
            "%s://%s%s",
            isset($_SERVER['HTTPS']) && $_SERVER['HTTPS'] != 'off' ? 'https' : 'http',
            $_SERVER['SERVER_NAME'],
            $_SERVER['REQUEST_URI']
        );

        $part = basename(__FILE__);
        $pos = strpos($baseURL, $part);
        $baseURL = substr_replace($baseURL, '', ($pos+strlen($part)));
        return $baseURL;
    }

    private function emulate_getallheaders()
    {
        foreach ($_SERVER as $name => $value) {
            if (substr($name, 0, 5) == 'HTTP_') {
                $name = str_replace(' ', '-', ucwords(strtolower(str_replace('_', ' ', substr($name, 5)))));
                $headers[$name] = $value;
            } else if ($name == "CONTENT_TYPE") {
                $headers["Content-Type"] = $value;
            } else if ($name == "CONTENT_LENGTH") {
                $headers["Content-Length"] = $value;
            }
        }

        return $headers;
    }

    private function processResponse($response)
    {
        $response = str_replace("HTTP/1.1 100 Continue\r\n\r\n", "", $response);
        $responseArray = explode("\r\n\r\n", $response, 2);
        $headers = $responseArray[0];
        $result = $responseArray[1];
        $headerArray = explode(chr(10), $headers);

        foreach ($headerArray as $header => $headerValue) {
            if (!preg_match("/^Transfer-Encoding/", $headerValue)) {
                $headerValue = str_replace($this->targetPath, $this->domainURL, $headerValue);
                header(trim($headerValue));
            }
        }

        echo $result;
    }

    private function disable_gzip()
    {
        @ini_set('zlib.output_compression', 'Off');
        @ini_set('output_handler', '');
    }
}

$acrolinxProxyInstance = new Proxy();
$acrolinxProxyInstance->processRequest();

?>
