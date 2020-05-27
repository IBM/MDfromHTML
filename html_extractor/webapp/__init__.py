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

from flask import Flask
from flask_restplus import Api

app = Flask(__name__)
api = Api(app = app, version = "1.1.1", title = "Capture HTML",
          description = "Capture HTML ")

test_ns = api.namespace('test')
ns = api.namespace('api')