<!doctype html>
<html lang="en">
  <head>
    <meta charset="utf-8" />
    <title>Acrolinx Proxy Sample</title>
  </head>
  <body>
    <h1>Acrolinx Proxy Sample</h1>
    <div>
      <button onclick="postFunction()">Sign in</button>
    </div>
    <div id="signInResult" />
    <script>
      async function postFunction() {
        const response = await postData('acrolinx-proxy-sample/proxy/api/v1/auth/sign-ins');

        if (response.status === 200) {
          const responseJson = await response.json();
          document.getElementById("signInResult").innerText = "Sign in success\n" + JSON.stringify(responseJson);
        } else if (response.status === 201) {
          const responseJson = await response.json();
          const interactiveURL = responseJson.links.interactive;
          document.getElementById("signInResult").innerText = "Complete Sign in by accessing interactive url & poll platform for success.";
          document.getElementById('signInResult').innerHTML = '<br/><a href="' + interactiveURL + '" target="_blank">Click here to complete sigin</a>';
        }
      }
  
      function postData(url) {
        return fetch(url, {
          method: 'GET',
          mode: 'cors',
          cache: 'no-cache',
          headers: {
            'X-Acrolinx-Client': 'SW50ZWdyYXRpb25EZXZlbG9wbWVudERlbW9Pbmx5; 1.0'
          },
          redirect: 'follow'
        });
      }
    </script>
  </body>
</html>
