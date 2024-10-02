<html>

<body>
    <h2>Acrolinx Proxy Sample</h2>
    <div>
        <button class="btnSSO" onclick="PostFunction()">SIGN IN</button>
    </div>
    <br />
    <div id="signInResult"></div>
    <br />
    <script>
        async function PostFunction() {
            try {
                const response = await postData('acrolinx-proxy-sample/proxy/api/v1/auth/sign-ins');
                if (response.status === 200) {
                    const respJson = await response.json();
                    document.getElementById("signInResult").innerText = "Signin success\n" + JSON.stringify(respJson);
                }
                else if (response.status === 201) {
                    const respJson = await response.json();
                    const interactiveURL = respJson.links.interactive;
                    document.getElementById("signInResult").innerText = "Complete Signin by accessing interactive url & poll platform for success.";
                    document.getElementById('signInResult').innerHTML = '<br/><a href="' + interactiveURL + '" target="_blank">Click here to complete sigin</a>';
                }
            } catch (error) {
                console.error(error);
            }
        }

        async function postData(url = '', data = {}) {
            const response = await fetch(url, {
                method: 'POST',
                mode: 'cors',
                cache: 'no-cache',
                headers: {
                    'Content-Type': 'application/json',
                    'X-Acrolinx-Client': "SW50ZWdyYXRpb25EZXZlbG9wbWVudERlbW9Pbmx5" + "; " + "1.0"
                },
                redirect: 'follow',
                body: JSON.stringify(data)
            });
            return response;
        }
    </script>
</body>

</html>