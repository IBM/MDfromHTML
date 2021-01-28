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

# added below for workaround from https://github.com/jarus/flask-testing/issues/143
try:
    from flask_restplus import Resource, Api
except ImportError:
    import werkzeug
    werkzeug.cached_property = werkzeug.utils.cached_property
    from flask_restplus import Resource, Api

from webapp import app
from webapp.routes import warmup

if __name__ == '__main__':
    warmup()
    print("Warming up..")
    app.run(host='0.0.0.0', port=os.getenv('PORT', 5000), debug=True, use_reloader=False)
    print("Web app ready to be launched!")
