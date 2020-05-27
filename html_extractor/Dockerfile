# Start with a python 3.7 image
FROM joyzoursky/python-chromedriver:3.7-alpine3.8-selenium

# Install dependices
RUN apk add --no-cache --virtual .build-deps gcc libc-dev libxslt-dev && \
    apk add --no-cache libxslt && \
    pip install --no-cache-dir lxml>=3.5.0 && \
    apk del .build-deps

# Document who is responsible for this image
MAINTAINER Nathaniel Mills "wnm3@us.ibm.com"

# Expose any ports the app is expecting in the environment
ENV PORT 5000
EXPOSE $PORT

# Set up a working folder and install the pre-reqs
WORKDIR /app
ADD requirements.txt /app
RUN pip install -r requirements.txt

# set chromedriver environment path
ENV CHROMEDRIVER_PATH /usr/local/bin/chromedriver

# Add the code as the last Docker layer because it changes the most
ADD webapp /app/webapp
ADD htmlcapture.py /app

# Run the service
CMD [ "python", "htmlcapture.py" ]
