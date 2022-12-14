<!--
  ~ Licensed to the Apache Software Foundation (ASF) under one or more
  ~ contributor license agreements.  See the NOTICE file distributed with
  ~ this work for additional information regarding copyright ownership.
  ~ The ASF licenses this file to You under the Apache License, Version 2.0
  ~ (the "License"); you may not use this file except in compliance with
  ~ the License.  You may obtain a copy of the License at
  ~
  ~   http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
  -->

<template>
  <div class="ecm" >
    <Search :statusList="healthyStatusList" :ownerList="ownerList" @search="search" />
    <Spin
      v-if="loading"
      size="large"
      fix/>
    <Table class="table-content ecm-table" border :width="tableWidth" :columns="columns" :data="pageDatalist">
      <template slot-scope="{row}" slot="instance">
        <a @click="getEngineConnList(row)">{{`${row.instance}`}}</a>
      </template>
      <template slot-scope="{row}" slot="usedResource">
        <span>{{ row.usedResource | formatResource }}</span>
      </template>
      <template slot-scope="{row}" slot="maxResource">
        <span>{{ row.maxResource | formatResource }}</span>
      </template>
      <template slot-scope="{row}" slot="lockedResource">
        <span>{{ row.lockedResource | formatResource }}</span>
      </template>
      <template slot-scope="{row}" slot="startTime">
        <span>{{ timeFormat(row) }}</span>
      </template>
      <template slot-scope="{row}" slot="labels">
        <Tooltip v-for="(item, index) in row.labels" :key="index" :content="`${item.stringValue}`" placement="top">
          <Tag type="border" color="primary">{{`${item.stringValue}`}}</Tag>
        </Tooltip>
      </template>
    </Table>
    <div class="page-bar">
      <Page
        ref="page"
        :total="page.totalSize"
        :page-size-opts="page.sizeOpts"
        :page-size="page.pageSize"
        :current="page.pageNow"
        class-name="page"
        size="small"
        show-total
        show-sizer
        :prev-text="$t('message.linkis.previousPage')" :next-text="$t('message.linkis.nextPage')"
        @on-change="change"
        @on-page-size-change="changeSize" />
    </div>
    <Modal
      @on-ok="submitTagEdit"
      @on-visible-change="resetTagAdd"
      :title="$t('message.linkis.tagEdit')"
      v-model="isTagEdit"
      :mask-closable="false">
      <Form :label-width="80">
        <FormItem :label="`${$t('message.linkis.instanceName')}???`">
          <Input disabled v-model="formItem.instance" />
        </FormItem>
        <FormItem class="addTagClass" :label="`${$t('message.linkis.tableColumns.label')}???`">
          <WbTag ref="wbtags" :tagList="formItem.labels" :selectList="keyList" @addEnter="addEnter" @onCloseTag="onCloseTag" @editEnter="editEnter" ></WbTag>
        </FormItem>
        <FormItem :label="`${$t('message.linkis.tableColumns.status')}???`">
          <Select v-model="formItem.emStatus">
            <Option
              v-for="(item) in healthyStatusList"
              :label="item"
              :value="item"
              :key="item"/>
          </Select>
        </FormItem>
      </Form>
    </Modal>
  </div>
</template>
<script>
import api from '@/common/service/api';
import moment from "moment";
import Search from '@/apps/linkis/module/ECM/search.vue';
import WbTag from '@/apps/linkis/components/tag';
export default {
  name: 'ECM',
  data() {
    return {
      keyList: [],
      statusList: [], // ????????????????????????
      healthyStatusList: [], // ????????????????????????
      ownerList: [],
      loading: false,
      formItem: {
        instance: '',
        labels: [],
        emStatus: '',
        applicationName: '',
      },
      tagTitle: [],
      addTagForm: { // ???????????????form??????
        key: '',
        value: ''
      },
      isShowTable: false,
      addTagFormRule: { // ????????????
        key: [
          { required: true, message: this.$t('message.linkis.keyTip'), trigger: 'blur' }
        ]
      },
      tableWidth: 0,
      // ????????????????????????
      isTagEdit: false,
      tableData: [],
      page: {
        totalSize: 0,
        sizeOpts: [15, 30, 45],
        pageSize: 15,
        pageNow: 1
      },
      columns: [
        {
          title: this.$t('message.linkis.tableColumns.instanceName'), // ????????????
          key: 'instance',
          minWidth: 150,
          className: 'table-project-column',
          slot: 'instance'
        },
        {
          title: this.$t('message.linkis.tableColumns.status'), // ??????
          key: 'nodeHealthy',
          minWidth: 100,
          className: 'table-project-column',
        },
        {
          title: this.$t('message.linkis.tableColumns.label'), // ??????
          key: 'labels',
          minWidth: 160,
          className: 'table-project-column',
          slot: 'labels'
        },
        {
          title: this.$t('message.linkis.tableColumns.usedResources'), // ????????????
          key: 'usedResource',
          className: 'table-project-column',
          slot: 'usedResource',
          minWidth: 150,
        },
        {
          title: this.$t('message.linkis.tableColumns.maximumAvailableResources'), // ??????????????????
          key: 'maxResource',
          slot: 'maxResource',
          className: 'table-project-column',
          minWidth: 150,
        },
        {
          title: this.$t('message.linkis.tableColumns.lockedResource'), // ??????????????????
          key: 'lockedResource',
          slot: 'lockedResource',
          className: 'table-project-column',
          minWidth: 150,
        },
        {
          title: this.$t('message.linkis.tableColumns.initiator'), // ?????????
          key: 'owner',
          className: 'table-project-column',
          minWidth: 150,
        },
        {
          title: this.$t('message.linkis.tableColumns.startTime'), // ????????????
          key: 'startTime',
          className: 'table-project-column',
          slot: 'startTime',
          minWidth: 150,
        },
        {
          title: this.$t('message.linkis.tableColumns.control.title'),
          key: 'action',
          width: '80',
          // fixed: 'right',
          align: 'center',
          render: (h, params) => {
            return h('div', [
              h('Button', {
                props: {
                  type: 'primary',
                  size: 'small'
                },
                on: {
                  click: () => {
                    this.isTagEdit = true;
                    let obj = {};
                    obj.instance = params.row.instance;
                    let labels = params.row.labels || [];
                    // ?????????????????????????????????????????????
                    obj.labels = labels.map(item => {
                      return {
                        key: item.labelKey,
                        value: item.stringValue,
                        modifiable: item.modifiable || false,
                      }
                    })
                    obj.emStatus = params.row.nodeHealthy;
                    obj.applicationName = params.row.applicationName;
                    this.formItem = Object.assign(this.formItem, obj)
                  }
                }
              }, this.$t('message.linkis.tagEdit'))
            ]);
          }
        }
      ],
    }
  },
  components: {
    Search,
    WbTag
  },
  computed: {
    pageDatalist() {// ???????????????
      return this.tableData.filter((item, index) => {
        return (this.page.pageNow - 1) * this.page.pageSize <= index && index < this.page.pageNow * this.page.pageSize;
      })
    }
  },
  filters: {
    formatResource(v) {
      const calcCompany = function(num, isCompany = false) {
        let data = num > 0 ? num : 0;
        if (isCompany) {
          return data / 1024 / 1024 / 1024;
        }
        return data;
      }
      return  v && (v.cores !== undefined || v.memory !== undefined || v.instance !== undefined) ? `${calcCompany(v.cores)}cores,${calcCompany(v.memory, true)}G,${calcCompany(v.instance)}apps` : ''
    }
  },
  created() {
    this.initECMList();
    // ????????????????????????
    this.getListAllNodeHealthyStatus();
    this.getSearchStatus();
    this.getKeyList();
  },
  methods: {

    // ????????????
    refreshResource() {
      this.initECMList();
    },
    // ?????????ECM??????
    async initECMList() {
      // ??????ECM????????????
      this.loading = true;
      try {
        let ECM = await api.fetch('/linkisManager/listAllEMs','get') || {};
        // ?????????????????????????????????
        let ECMList = ECM.EMs || [];
        this.tableData = ECMList;
        this.ownerList = [];
        ECMList.forEach(item => {
          if(this.ownerList.indexOf(item.owner) === -1) {
            this.ownerList.push(item.owner)
          }
        })
        this.page.totalSize = this.tableData.length;
        this.loading = false;
      } catch (err) {
        console.log(err)
        this.loading = false;
      }
    },
    // ????????????????????????labelKey
    getKeyList() {
      api.fetch('/microservice/modifiableLabelKey', 'get').then((res) => {
        let list = res.keyList || [];
        this.keyList = list.map(item => {
          return {
            lable: item,
            value: item
          }
        })
      })
    },
    // ????????????????????????????????????
    async getListAllNodeHealthyStatus() {
      try {
        let healthyStatusList = await api.fetch('/linkisManager/listAllECMHealthyStatus', { onlyEditable: true }, 'get') || {};

        let list = healthyStatusList.nodeHealthy || [];
        this.healthyStatusList = [...list];
      } catch (err) {
        console.log(err)
      }
    },
    // ???????????????????????????
    async getSearchStatus() {
      try {
        let statusList = await api.fetch('/linkisManager/listAllNodeHealthyStatus', 'get') || {};
        let list = statusList.nodeStatus || [];
        this.statusList = [...list];
      } catch (err) {
        console.log(err)
      }
    },
    // ??????tag
    addEnter (key, value) {
      this.formItem.labels.push({ key, value });
    },

    // ??????tag
    editEnter(editInputKey, editInputValue,editedInputValue) {
      let index = this.formItem.labels.findIndex((item)=>{
        return  item.value === editInputValue
      })
      this.formItem.labels.splice(index,1,{key: editInputKey,modifiable: true,value: editedInputValue})
    },

    // ??????tag
    onCloseTag (name, index) {
      this.formItem.labels.splice(index, 1);
    },

    //  ????????????
    submitTagEdit() {
      let param = JSON.parse(JSON.stringify(this.formItem));
      param.labels = param.labels.map(item => {
        return {
          labelKey: item.key,
          stringValue: item.value,
        }
      })
      api.fetch('/linkisManager/modifyEMInfo', param, 'put').then(() => {
        this.isTagEdit = false;
        this.$Message.success(this.$t('message.linkis.editedSuccess'));
        this.refreshResource(); // ??????
      }).catch(() => {
        this.isTagEdit = false;
      })
    },
    // ????????????
    change(val) {
      this.page.pageNow = val;
    },
    // ???????????????
    changeSize(val) {
      this.page.pageSize = val;
      this.page.pageNow = 1;
    },
    // ??????
    search(e) {
      let param = {
        instance: e.instance,
        nodeHealthy: e.nodeHealthy,
        owner: e.owner
      }
      api.fetch('/linkisManager/listAllEMs',param,'get').then((res)=>{
        this.tableData = res.EMs
      })


      this.page.pageNow = 1;
      this.page.totalSize = this.tableData.length;
    },
    // ?????????????????????
    getEngineConnList(e) {
      this.$router.push({ name: 'EngineConnList', query: { instance: e.instance, applicationName: e.applicationName } })
    },
    // ??????????????????
    timeFormat(row) {
      return moment(new Date(row.startTime)).format('YYYY-MM-DD HH:mm:ss')
    },
    resetTagAdd(v) {
      if (v===false && this.$refs.wbtags) {
        this.$refs.wbtags.resetTagAdd()
      }
    }
  }
}
</script>

<style src="./index.scss" lang="scss" scoped></style>

<style lang="scss">
.ecm-table {
  border: 0;
  height: calc(100% - 110px);
  overflow: auto;

  .ivu-table:before {
    height: 0
  }
  
  .ivu-table:after {
    width: 0
  }

  .ivu-table {
    height: auto;
    border: 1px solid #dcdee2;
  }
}
</style>