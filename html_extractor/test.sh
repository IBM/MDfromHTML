#! /bin/bash
curl -X GET "http://0.0.0.0:5000/APIs/get_generic_content?url=http://www.google.com" -H "accept: application/json" >> Test.text
