package tests.rest.v2.storage

import base.RESTSpec
import spock.lang.IgnoreIf

import static config.Config.*

class FilesSpec extends RESTSpec{


    def storageId

    def setup(){
        setUser(ADMIN_USERNAME, ADMIN_PASSWORD)
        def responseDataAll = get(PATH_FILES)
        responseDataAll.files.each{
            delete(PATH_FILES + "/${it.id}")
        }

        responseDataAll = get(PATH_STORAGE)
        responseDataAll.storageSystems.each{
            delete(PATH_STORAGE + "/${it.id}")
        }

        def sourceSystem = [
                'name':'Arvbox at The Hyve',
                'systemType':'Arvados',
                'url':'http://arvbox-pro-dev.thehyve.net/',
                'systemVersion':'v1',
                'singleFileCollections':false,
        ]
        def responseData = post(PATH_STORAGE, toJSON(sourceSystem))
        storageId = responseData.id
    }

    /**
     *  post, get, put, delete
     */
    def "post, get, put, delete"(){
        given:
        def new_file_link = [
                'name'        : 'new file Link',
                'sourceSystem': storageId,
                'study'       : 'EHR',
                'uuid'        : 'aaaaa-bbbbb-ccccccccccccccc',
        ]

        when:
        def responseData = post(PATH_FILES, toJSON(new_file_link))
        def id = responseData.id

        then:
        assert responseData.id != null
        assert responseData.name == 'new file Link'
        assert responseData.sourceSystem == storageId
        assert responseData.study == 'EHR'
        assert responseData.uuid == 'aaaaa-bbbbb-ccccccccccccccc'

        when:
        responseData = get(PATH_FILES + "/${id}")

        then:
        assert responseData.id == id
        assert responseData.name == 'new file Link'
        assert responseData.sourceSystem == storageId
        assert responseData.study == 'EHR'
        assert responseData.uuid == 'aaaaa-bbbbb-ccccccccccccccc'

        when:
        def responseDataAll = get(PATH_FILES)

        then:
        assert responseDataAll.files.contains(responseData)

        when:
        new_file_link.name = 'new file Link renamed'
        responseData = put(PATH_FILES + "/${id}", toJSON(new_file_link))

        then:
        assert responseData.id == id
        assert responseData.name == 'new file Link renamed'
        assert responseData.sourceSystem == storageId
        assert responseData.study == 'EHR'
        assert responseData.uuid == 'aaaaa-bbbbb-ccccccccccccccc'

        when:
        responseData = delete(PATH_FILES + "/${id}")
        assert responseData == null
        responseData = get(PATH_FILES + "/${id}")

        then:
        assert responseData.status == 404
        assert responseData.error == 'Not Found'
        assert responseData.message == 'No message available'
        assert responseData.path == "/${PATH_FILES}/${id}"
    }


    /**
     *  post invalid
     */
    def "post invalid values"() {
        given:
        def new_file_link = [
                'name'        : 'new file Link',
                'sourceSystem': storageId,
                'study'       : null,
                'uuid'        : 'aaaaa-bbbbb-ccccccccccccccc',
        ]

        when:
        def responseData = post(PATH_FILES, toJSON(new_file_link))

        then:
        assert responseData.httpStatus == 500
        assert responseData.message == 'No such property: transactionStatus for class: org.transmartproject.rest.StorageController'
        assert responseData.type == 'MissingPropertyException'
    }

    /**
     *  post empty
     */
    def "post empty"() {
        when:
        def responseData = post(PATH_FILES, null)

        then:
        assert responseData.httpStatus == 500
        assert responseData.message == 'No such property: transactionStatus for class: org.transmartproject.rest.StorageController'
        assert responseData.type == 'MissingPropertyException'
    }

    /**
     *  get nonexistent
     */
    def "get nonexistent"() {
        when:
        def responseData = get(PATH_FILES + "/0")

        then:
        assert responseData.status == 404
        assert responseData.error == 'Not Found'
        assert responseData.message == 'No message available'
        assert responseData.path == "/${PATH_FILES}/0"
    }

    /**
     *  put invalid
     */
    def "put invalid"() {
        given:
        def new_file_link = [
                'name'        : 'new file Link',
                'sourceSystem': storageId,
                'study'       : 'EHR',
                'uuid'        : 'aaaaa-bbbbb-ccccccccccccccc',
        ]
        def responseData = post(PATH_FILES, toJSON(new_file_link))
        def id = responseData.id
        new_file_link.uuid = null

        when:
        responseData = put(PATH_FILES +"/${id}", toJSON(new_file_link))

        then:
        assert responseData.errors.size() == 1
        assert responseData.errors[0].field == 'uuid'
        assert responseData.errors[0].message == 'Property [uuid] of class [class org.transmartproject.db.storage.LinkedFileCollection] cannot be null'
        assert responseData.errors[0].'rejected-value' == null
        assert responseData.errors[0].object == 'org.transmartproject.db.storage.LinkedFileCollection'

    }

    /**
     *  put nonexistent
     */
    @IgnoreIf({SUPPRESS_KNOWN_BUGS}) //returns 500 insted of not found
    def "put nonexistent"() {
        given:
        def new_file_link = [
                'name'        : 'new file Link',
                'sourceSystem': storageId,
                'study'       : 'EHR',
                'uuid'        : 'aaaaa-bbbbb-ccccccccccccccc',
        ]

        when:
        def responseData = put(PATH_FILES +"/0", toJSON(new_file_link))

        then:
        assert responseData.status == 404
        assert responseData.error == 'Not Found'
        assert responseData.message == 'No message available'
        assert responseData.path == "/${PATH_FILES}/0"
    }

    /**
     *  no access
     */
    def "no access"(){
        given:
        setUser(DEFAULT_USERNAME, DEFAULT_PASSWORD)
        def new_file_link = [
                'name'        : 'new file Link',
                'sourceSystem': storageId,
                'study'       : 'EHR',
                'uuid'        : 'aaaaa-bbbbb-ccccccccccccccc',
        ]

        when:
        def responseData = post(PATH_FILES, toJSON(new_file_link))

        then:
        assert responseData.httpStatus == 403
        assert responseData.message == 'Creating new Linked File Collections is an admin action'
        assert responseData.type == 'AccessDeniedException'

        when:
        new_file_link.name = 'new file Link renamed'
        responseData = put(PATH_FILES + "/0", toJSON(new_file_link))

        then:
        assert responseData.httpStatus == 403
        assert responseData.message == 'updating a linked file entry is an admin action'
        assert responseData.type == 'AccessDeniedException'

        when:
        responseData = delete(PATH_FILES + "/0")

        then:
        assert responseData.httpStatus == 403
        assert responseData.message == 'Removing a linked file entry is an admin action'
        assert responseData.type == 'AccessDeniedException'
    }

}
