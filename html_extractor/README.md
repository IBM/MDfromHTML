# HTML Extractor

### Setting up the environment

1. Create a Python 3 virtual environment:
  * Using Python commands easier and uses these commands:
```bash
python3 -m venv htmlcapture
source ./htmlcapture/bin/activate

Note: to deactivate simply type: deactivate though the rest of these instructions assume you have activated the virtual environment
```
  * Alternatively, use Anaconda 3 which can be downloaded from [here](https://anaconda.com)
```bash
conda create --name html python=3.7
conda activate html
```
2. Install requirements
```bash
pip install --upgrade pip
pip install -r requirements.txt
```
3. Download chromedriver [here](https://chromedriver.storage.googleapis.com/72.0.3626.69/chromedriver_mac64.zip) and set the environment variable
```bash
export CHROMEDRIVER_PATH=/path/to/chromedriver
```
4. The code also uses MongoDB to save html content. Please install it from [here](https://docs.mongodb.com/v3.2/administration/install-community/)

### Starting the server
1. Start the server (you will see a chrome browser open, don't close it)
```bash
python htmlcapture.py
```
Give it a couple of seconds to warm up. Wait until you see
```bash
Warming up..
Web app ready to be launched!
 * Serving Flask app "webapp" (lazy loading)
 * Environment: production
   WARNING: Do not use the development server in a production environment.
   Use a production WSGI server instead.
 * Debug mode: on
 * Running on http://0.0.0.0:5000/ (Press CTRL+C to quit)
```
2. Call the API
```bash
curl -X GET "http://localhost:5000/api/get_generic_content?url=https://example.com" -H "accept: application/json"
```
3. Sample response
```json
{
"initiator": "Nathaniel Mills", 
"initiatorEmail": "wnm3@us.ibm.com", 
"captureDate": "2020/05/27", 
"caputreTime": "16:48:58", 
"captureUtility": "https://github.com/IBM/MDfromHTML/html_capture", 
"captureArray": [{
  "url": "https://example.com", 
  "html": "<html><body><h1>Example website</h1>\n <p>\nSample content.</p></body></html>", 
  "content": "Example website\n \nSample content." 
  }]
}
```
