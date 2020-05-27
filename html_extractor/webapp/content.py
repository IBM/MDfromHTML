# /**
#  * (c) Copyright 2020 IBM Corporation
#  * 1 New Orchard Road, 
#  * Armonk, New York, 10504-1722
# * United States
# * +1 914 499 1900
# * support: Nathaniel Mills wnm3@us.ibm.com
# *
# * Licensed under the Apache License, Version 2.0 (the "License");
# * you may not use this file except in compliance with the License.
# * You may obtain a copy of the License at
# *
# *    http://www.apache.org/licenses/LICENSE-2.0
# *
# * Unless required by applicable law or agreed to in writing, software
# * distributed under the License is distributed on an "AS IS" BASIS,
# * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# * See the License for the specific language governing permissions and
# * limitations under the License.
# *
# */

import os
import datetime
import time
import json
import requests

from webapp.exceptions import ContentTypeException

from selenium import webdriver
from selenium.common.exceptions import TimeoutException
# from xvfbwrapper import Xvfb
from bs4 import BeautifulSoup

from pymongo import MongoClient, ReturnDocument
from pymongo.errors import OperationFailure

import ssl
import tldextract

MONGO_SERVICE = os.getenv('MONGO_SERVICE_HOST', 'localhost')
client = MongoClient(MONGO_SERVICE, ssl_cert_reqs=ssl.CERT_NONE)
USERNAME = os.getenv('USERNAME', '')
PASSWORD = os.getenv('PASSWORD', '')

# vdisplay = Xvfb()
# vdisplay.start()

chrome_options = webdriver.ChromeOptions()
# chrome_options.add_argument('--no-sandbox')
# chrome_options.add_argument('--window-size=1420,1080')
# chrome_options.add_argument('--headless')
# chrome_options.add_argument('--disable-gpu')

with open("webapp/sample_post_request_payload.json", 'r') as f:
    data = json.load(f)
    HTML_FILTERS = data

class ContentExtractor:
    def __init__(self, chromedriver_path):
        self.chromedriver = chromedriver_path
        self.driver = webdriver.Chrome(self.chromedriver, options=chrome_options)

        # Help@IBM  web page authentication flag
        self.ibm_auth = False

        # Instantiate database
        self.db = client['html_content']


    def get_collection_name_counts(self):
        filter = {"name": {"$regex": r"^(?!system\.)"}}
        colls = self.db.list_collection_names(filter=filter)
        return {name: self.db[name].estimated_document_count() for name in colls}


    def get_collection_stats(self, collection):
        coll = self.db[collection]
        results = list(coll.find({}, {'_id': False, "url": True}))
        return [result["url"] for result in results]


    def get_generic_content(self, url):
        self.driver.get(url)
        time.sleep(5)

        innerHTML = self.driver.page_source

        soup = BeautifulSoup(innerHTML, 'lxml')

        return soup, soup.text


    def get_ibm_help_content(self, url):

        if not self.ibm_auth:
            self.driver.get(url)
            print("trying to auth")
            time.sleep(3)
            self.driver.find_element_by_link_text("Not now").click()
            time.sleep(5)
            self.ibm_auth = True

        self.driver.get(url)
        time.sleep(3)
        # innerHTML = self.driver.execute_script("return document.body.innerHTML")
        innerHTML = self.driver.page_source
        soup = BeautifulSoup(innerHTML, 'lxml')
        # content = soup.find("div", {"id": "content"})

        text = soup.text

        if "What do you need help with?" in text:
            try:
                self.driver.find_element_by_xpath("//*[@id=\"not-found\"]/div/div/div[1]/button").click()
                time.sleep(1)
            except:
                pass

            text = "Content Not Found"

        return soup, text


    def get_ibm_help_content_login(self, url):
        self.driver.get(url)
        time.sleep(1)

        if not self.ibm_auth:
            time.sleep(1)
            self.driver.find_element_by_xpath("//*[@id=\"sign-in-modal\"]/div/div/div[2]/div[2]/button").click()
            time.sleep(5)
            self.driver.find_element_by_name("username").send_keys(USERNAME)
            self.driver.find_element_by_name("password").send_keys(PASSWORD)
            time.sleep(1)
            self.driver.find_element_by_xpath("//*[@id=\"btn_signin\"]").click()
            time.sleep(3)
            self.ibm_auth = True

        innerHTML = self.driver.execute_script("return document.body.innerHTML")
        soup = BeautifulSoup(innerHTML, 'lxml')

        text = soup.text

        if "What do you need help with?" in text:
            try:
                self.driver.find_element_by_xpath("//*[@id=\"not-found\"]/div/div/div[1]/button").click()
                time.sleep(1)
            except:
                pass

            text = "Content Not Found"

        return soup, text


    def get_content(self, url, type = 'generic', check_db=True, cache_refresh=True):
        now = datetime.datetime.utcnow()
        ext = tldextract.extract(url)
        domain_name = '.'.join(part for part in ext[:2] if part)
        try:
            coll = self.db[domain_name]
        except Exception as e:
            return {"message": "Invalid URL attempted - {}".format(url),
                    "error": e}

        if cache_refresh:
            check_db = False

        if check_db:
            # First check database if extracted content exists
            result = list(coll.find({"url": url}, {'_id': False}))

            if len(result) == 1:
                print("Found extracted content in cache for url - {}".format(url))
                return result[0]
            else:
                print("Extracting content for url - {}".format(url))

        # Define content extractor
        if type == 'generic':
            extractor = self.get_generic_content
        elif type == 'helpatibm':
            extractor = self.get_ibm_help_content
        elif type == 'helpatibm_login':
            extractor = self.get_ibm_help_content_login
        else:
            raise ContentTypeException

        try:
            url_html_content, text = extractor(url)
        except TimeoutException:
            print("Time out exception while loading ")
            raise TimeoutException
        except Exception as e:
            print(e)
            raise Exception

        json_content = {
            "url": url,
            "initiator": "Nathaniel Mills",
            "initiatorEmail": "wnm3@us.ibm.com",
            "captureDate": now.strftime("%Y/%m/%d"),
            "caputreTime": now.strftime("%H:%M:%S"),
            "captureUtility": "https://github.com/IBM/MDfromHTML/html_extractor",
            "captureArray": [
                {
                    "url": url,
                    # "outer_html": str(url_outer_html),
                    "html": str(url_html_content),
                    "content": text
                }
            ]
        }

        # If cache refresh requested, replace old data
        if cache_refresh:
            print("Replacing old content for {}".format(url))
            coll.replace_one(
                {"url": url},
                json_content
            )

        # Add extracted content to database
        if check_db:
            coll.insert_one(json_content)
            result = list(coll.find({"url": url}, {'_id': False})) #
            return result[0]

        return json_content


    def get_markdown(self, json_content):

        # Create request object for extracting Markdown using Nat's service
        markdown_url = 'https://129.34.40.111/AidenWebServices/v1/doc2dial/getMarkdown'
        markdown_request_obj = HTML_FILTERS
        markdown_request_obj["request"]["url"] = json_content["captureArray"][0]["url"]
        markdown_request_obj["request"]["html"] = json_content["captureArray"][0]["html"]
        headers = {"Content-Type": "application/json"}
        x = requests.post(markdown_url, json=markdown_request_obj, headers=headers)
        return x.json()

if __name__=="__main__":
    # For testing, ignore these lines
    CHROMEDRIVER_PATH = os.getenv("CHROMEDRIVER_PATH", "/usr/local/bin/chromedriver")

    content_extractor = ContentExtractor(chromedriver_path=CHROMEDRIVER_PATH)

    response = content_extractor.get_content(url="http://www.google.com",
                                             type='generic')
    print(response)