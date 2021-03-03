# Changes to the original Remark

## Codebase

 * Removed spurious ignore directives.
 * Added templates for Java 1.8

## Functional Changes

#### 20210215 Changes
 * General bug fixes for stack overflow

#### 20190814 Changes
 * Added AnnotationWriter class and added it throughout the Remark API's to prepare for tracking the Nodes/Elements used to generate a markdown output line
 * Added filters for id's and aria-label's that reference footer
 * Added a cleaner to skip all lines until a header of some form is encountered.

#### Initial Work:
 * Added class and aria-hidden to the :all attributes in the Whitelist so we'd have visibility in the code to consider filters.
 * Added filters for classes containing the word "hidden" and for attributes keyed by "aria-hidden" with "true" values.
 * Added clean up of markdown to remove any empty list items resulting from filtering by classes referencing hidden, or attributes with aria-hidden
 
 