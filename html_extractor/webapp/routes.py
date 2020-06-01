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

from flask import request, render_template, make_response
from webapp import app, api, ns, test_ns
from webapp.content import ContentExtractor, TimeoutException

from flask_restplus import Resource

CHROMEDRIVER_PATH = os.getenv("CHROMEDRIVER_PATH", "/usr/local/bin/chromedriver")

content_extractor = ContentExtractor(chromedriver_path=CHROMEDRIVER_PATH)

def warmup():
    response = content_extractor.get_content(url="http://www.google.com",
                                             type='generic')

@app.route('/')
@app.route('/main')
def urls():
    return render_template('layouts/default.html',
                           content=render_template('pages/index.html',
                                                   collections=content_extractor.get_collection_name_counts()
                                                   )
                           )


@app.route('/<domain_name>')
def domain_stats(domain_name):
    return render_template('layouts/default.html',
                           content=render_template('pages/urls.html',
                                                   collection_name=domain_name,
                                                   collections=content_extractor.get_collection_stats(domain_name)
                                                   )
                           )


@app.route('/index')
class Index(Resource):
    def get(self):
        return "Hello, World!"


@ns.route("/get_generic_content")
class GetGenericContent(Resource):

    @api.doc(responses={ 200: 'OK', 400: 'Invalid Argument', 500: 'Time Out Exception' },
             params={ 'url': 'Specify the url' })
    def get(self):
        try:
            print(request.args)
            url = request.args.get('url')
            response = content_extractor.get_content(url, type = 'generic')
            return response

        except TimeoutException as e:
            # Reinitialize the webdriver
            content_extractor.__init__(chromedriver_path=CHROMEDRIVER_PATH)
            ns.abort(500, " - {}".format(url), status = "Time out exception", statusCode = "500")
        except Exception as e:
            ns.abort(400, " - {}".format(url), status = "Could not retrieve information", statusCode = "400")


@ns.route("/get_ibm_help_content")
class GetIBMHelpContent(Resource):

    @api.doc(responses={ 200: 'OK', 400: 'Invalid Argument', 500: 'Time Out Exception' },
             params={ 'url': 'Specify the url' })
    def get(self):
        try:
            print(request.args)
            url = request.args.get('url')
            response = content_extractor.get_content(url, type = 'helpatibm')
            return response

        except TimeoutException as e:
            # Reinitialize the webdriver
            content_extractor.__init__(chromedriver_path=CHROMEDRIVER_PATH)
            ns.abort(500, " - {}".format(url), status = "Time out exception", statusCode = "500")
        except Exception as e:
            ns.abort(400, " - {}".format(url), status = "Could not retrieve information", statusCode = "400")


@test_ns.route("/request_generic_content")
class RequestGenericContent(Resource):

    @api.doc(responses={ 200: 'OK', 400: 'Invalid Argument', 500: 'Time Out Exception' },
             params={ 'url': 'Specify the url' })
    def get(self):
        try:
            print(request.args)
            url = request.args.get('url')
            _ = content_extractor.get_content(url, type = 'generic')
            headers = {'Content-Type': 'text/html'}
            return make_response(render_template('layouts/default.html',
                                   content=render_template('pages/index.html',
                                                           collections=content_extractor.get_collection_name_counts()
                                                           )
                                                 ),
                                   200,
                                   headers
                                   )

        except TimeoutException as e:
            # Reinitialize the webdriver
            content_extractor.__init__(chromedriver_path=CHROMEDRIVER_PATH)
            ns.abort(500, " - {}".format(url), status = "Time out exception", statusCode = "500")
        except Exception as e:
            ns.abort(400, " - {}".format(url), status = "Could not retrieve information", statusCode = "400")


@test_ns.route("/get_markdown")
class GetMarkdown(Resource):

    @api.doc(responses={ 200: 'OK', 400: 'Invalid Argument', 500: 'Time Out Exception' },
             params={ 'url': 'Specify the url' })
    def get(self):

        try:
            # print(request.args)

            # Get html content
            url = request.args.get('url')
            html = content_extractor.get_content(url, type='generic')
            response = content_extractor.get_markdown(html)

            return response

        except TimeoutException as e:
            # Reinitialize the webdriver
            content_extractor.__init__(chromedriver_path=CHROMEDRIVER_PATH)
            ns.abort(500, e.__doc__ + " - {}".format(url), status="Time out exception", statusCode="500")
        except Exception as e:
            ns.abort(400, e.__doc__ + " - {}".format(url), status="Could not retrieve information", statusCode="400")
