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
  <div style="position:relative;height:100%">
    <div
      class="table-div"
      :style="{height:`${tableHeightCalc}px`}"
    >
      <div
        id="topDiv"
        class="table-top"
        :style="{right:`${(scrollBarWidthCalc())}px`}"
        @dblclick="headDblclick"
      >
        <table
          id="topTable"
          cellpadding="0"
          cellspacing="0"
          border="0"
          style="width:100%;table-layout:fixed;"
        >
          <tr>
            <td
              v-for="(top,index) in columns"
              class="top-td"
              :key="index"
              :data-col-index="index"
              :style="{width: top.width? `${top.width}px` : 'auto'}"
            >
              <span :data-col-index="index" :title="top.colHeadHoverTitle">{{top.title?top.title:""}}</span>
              <span
                :class="'sort-icon'"
                v-if="top.sortable"
              >
                <i class="ivu-icon ivu-icon-md-arrow-dropup"
                  :class="{'on': sortType.index === index && sortType.type === 'asc'}" @click="handleSort(index, 'asc')"></i>
                <i class="ivu-icon ivu-icon-md-arrow-dropdown"
                  :class="{'on': sortType.index === index && sortType.type === 'desc'}" @click="handleSort(index, 'desc')"></i>
              </span>
            </td>
          </tr>
        </table>
      </div>
      <div
        id="bottomDiv"
        class="table-bottom"
        @scroll.stop="handleScroll"
      >
        <div :style="{height:`${dataTop}px`}"></div>
        <table
          id="bottomTable"
          v-if="showTableList.length"
          cellpadding="0"
          cellspacing="0"
          border="0"
          style="width:100%;table-layout:fixed;"
          :style="{height:`${loadedNum*tdHeight}px`}"
        >
          <tr
            v-for="(items,indexs) in showTableList"
            @click="rowClick(items,indexs+dataTop/tdHeight+1)"
            @dblclick="rowDblclick(items,indexs+dataTop/tdHeight+1)"
            :key="indexs"
            :style="{'line-height':`${tdHeight}px`}"
            :class="selectIndex==indexs?'trselect':'trhover'"
          >
            <td
              class="bottom-td"
              v-if="columns[0].type=='index'"
              :style="{width: columns[0].width? `${columns[0].width}px` : 'auto', height:`${tdHeight}px`}"
            >
              {{indexs+dataTop/tdHeight+1}}</td>
            <td
              class="bottom-td"
              v-if="columns[0].type=='select'"
            ></td>
            <template v-for="(item,index) in columnsBottom">
              <td
                v-if="item.key"
                class="bottom-td"
                :key="index"
                :style="{width: item.width?`${item.width}px`:'auto', height:`${tdHeight}px`}"
                :title="item.logic==undefined?items[item.key]:item.logic(items)"
              >
                {{item.logic==undefined?items[item.key]:item.logic(items)}}
              </td>
              <td
                v-if="item.slot"
                class="bottom-td"
                :key="index"
                :style="{width: item.width?`${item.width}px`:'auto', height:`${tdHeight}px`}"
              >
                <slot :name="item.slot" :row="items" :index="indexs+dataTop/tdHeight+1"></slot>
              </td>
            </template>
          </tr>
        </table>
        <div v-show="showTableList.length < 1" class="no-data-tip" :style="noDataStyle">
          ????????????
        </div>
        <div :style="{height:`${tableOtherBottom}px`}"></div>
      </div>
    </div>
    <div
      class="table-bottom-load"
      v-show="showLoad"
      :style="{right:`${(scrollBarWidthCalc())}px`,top:'40px',height:`${tableHeightCalc-40}px`}"
    >
      <svg
        class="icon loading "
        aria-hidden="true"
      >
        <use xlink:href="#icon-jiazai"></use>
      </svg>
      <div class="msg">?????????,?????????</div>
    </div>
  </div>
</template>

<script>
export default {
  name: "WbTable",
  props: {
    loadNum: {
      //??????????????????
      type: [Number, String],
      default() {
        return 50;
      }
    },
    tdHeight: {
      //????????????
      type: [Number, String],
      default() {
        return 40;
      }
    },
    tableHeight: {
      //????????????
      type: [Number, String],
    },
    tableList: {
      //??????????????????
      type: Array,
      default() {
        return [];
      }
    },
    columns: {
      //????????????????????????
      type: Array,
      default() {
        return [];
      }
    },
    showHeader: {
      type: Boolean,
      default: true
    },
    highlightRow: {},
    outerSort: {}, // ?????????????????????????????????????????????????????????
    scrollBarWidth: {
      type: Number,
      default(){
        return 8
      }
    }
  },
  data() {
    return {
      showLoad: false,
      showTableList: [], //???????????????????????????
      loadedNum: 0, //???????????????????????????
      dataTotal: 0, //???????????????
      dataTop: 0, //???????????????????????????
      scrollTop: 0, //?????????????????????
      scrollHeight: 0, //?????????????????????
      handleScroll: null,
      selectIndex: -1, //????????????
      sortType: {},
      noDataStyle: {
        width: '100%'
      }
    };
  },
  computed: {
    tableOtherBottom() {
      //??????????????????????????????
      return (
        this.dataTotal * this.tdHeight -
        this.dataTop -
        this.loadedNum * this.tdHeight
      );
    },
    columnsBottom() {
      if (this.columns[0].type != undefined) {
        return this.columns.slice(1, this.columns.length);
      } else {
        return this.columns;
      }
    },
    // ??????????????????????????????????????????????????????
    tableHeightCalc() {
      let dataHeightt = (this.tdHeight + 1) * this.loadedNum + 43;// 43????????????????????????, +1 ???border
      if(this.tableHeight) return this.tableHeight;
      if(this.tableList.length <= 0) return 100;
      return dataHeightt
    }
  },
  methods: {
    /**
     * @typedef {Object} Options -?????????
     * @property {Boolean} leading -????????????????????????????????????
     * @property {this} context -?????????
     **/
    //??????Proxy??????????????????
    proxy(
      func,
      time,
      options = {
        leading: true,
        context: null
      }
    ) {
      let timer;
      let _this = this;
      let handler = {
        apply(target, _, args) {
          //??????????????????
          let bottomScroll = document.getElementById("bottomDiv");
          let topScroll = document.getElementById("topDiv");
          if (bottomScroll.scrollTop == _this.scrollTop) {
            //????????????
            _this.handleScrollLeft(topScroll, bottomScroll);
            return;
          }
          // ?????????????????????????????????
          if (!options.leading) {
            if (timer) return;
            timer = setTimeout(() => {
              timer = null;
              Reflect.apply(func, options.context, args);
            }, time);
          } else {
            if (timer) {
              _this.needLoad(bottomScroll);
              clearTimeout(timer);
            }
            timer = setTimeout(() => {
              Reflect.apply(func, options.context, args);
            }, time);
          }
        }
      };
      return new Proxy(func, handler);
    },
    //?????????????????????
    needLoad(bottomScroll) {
      if (
        Math.abs(bottomScroll.scrollTop - this.scrollTop) >
        this.tdHeight * this.loadNum
      ) {
        this.showLoad = true; //???????????????
        this.scrollTop = bottomScroll.scrollTop;
      }
    },
    //????????????
    scrollProcessing() {
      // const last = $event && $event.last;
      const bottomScroll = document.getElementById("bottomDiv");
      // const topScroll = document.getElementById("topDiv");
      const direction = bottomScroll.scrollTop >= this.scrollTop; //????????????
      // if(this.needLoad(last,bottomScroll))return;
      //????????????????????????????????????
      this.scrollTop = bottomScroll.scrollTop;
      direction ? this.handleScrollBottom() : this.handleScrollTop();
      this.showLoad = false;
    },
    //?????????????????????
    handleScrollTop() {
      if (this.dataTop < this.scrollTop) {
        //???????????????????????????????????????????????????????????????
        this.handleScrollBottom();
        return;
      }
      //???????????????????????????????????????????????????
      if (this.dataTotal > this.loadNum) {
        const computeHeight = this.dataTop; //????????????????????????????????????
        const maxHeigth = computeHeight - this.loadNum * this.tdHeight; //????????????????????????????????????
        if (this.scrollTop < computeHeight && this.scrollTop >= maxHeigth) {
          //?????????????????????????????????????????????
          const dataTopNum = parseInt(this.dataTop / this.tdHeight); //??????????????????
          dataTopNum - this.loadNum >= 0
            ? this.dataProcessing(
              this.loadNum,
              this.loadedNum - this.loadNum,
              "top"
            )
            : this.dataProcessing(dataTopNum, dataTopNum, "top");
        } else if (this.scrollTop < maxHeigth) {
          const scrollNum = parseInt(this.scrollTop / this.tdHeight); //?????????????????????????????????
          scrollNum - this.loadNum >= 0
            ? this.dataProcessing(this.loadNum * 2, scrollNum, "topAll")
            : this.dataProcessing(
              scrollNum + this.loadNum,
              scrollNum,
              "topAll"
            );
        }
      }
    },
    //?????????????????????
    handleScrollBottom() {
      if (this.dataTop > this.scrollTop) {
        this.handleScrollTop();
        return;
      }
      const computeHeight =
        this.dataTop + this.loadedNum * this.tdHeight - this.tableHeightCalc; //????????????????????????????????????
      const maxHeight = computeHeight + this.tdHeight * this.loadNum; //????????????????????????????????????
      if (this.scrollTop > computeHeight && this.scrollTop <= maxHeight) {
        //????????????????????????????????????????????????
        if (this.dataTotal > this.loadedNum) {
          const dataTopNum = parseInt(this.dataTop / this.tdHeight); //??????????????????
          const total = dataTopNum + this.loadedNum + this.loadNum;
          const otherTotal = this.dataTotal - (dataTopNum + this.loadedNum);
          total <= this.dataTotal
            ? this.dataProcessing(
              this.loadedNum - this.loadNum,
              this.loadNum,
              "bottom"
            )
            : this.dataProcessing(otherTotal, otherTotal, "bottom");
        }
      } else if (this.scrollTop > maxHeight) {
        let scrollNum = parseInt(this.scrollTop / this.tdHeight); //?????????????????????????????????
        scrollNum + this.loadNum <= this.dataTotal
          ? this.dataProcessing(scrollNum, this.loadNum * 2, "bottomAll")
          : this.dataProcessing(
            scrollNum,
            this.dataTotal - scrollNum + this.loadNum,
            "bottomAll"
          );
      }
    },
    //?????????????????????
    handleScrollLeft(topScroll, bottomScroll) {
      //??????????????????????????????
      topScroll.scrollTo(bottomScroll.scrollLeft, topScroll.pageYOffset);
    },
    //???????????????????????????
    dataProcessing(topNum, bottomNum, type) {
      const topPosition = parseInt(this.dataTop / this.tdHeight);
      if (type === "top") {
        this.showTableList.splice(this.loadedNum - bottomNum, bottomNum); //??????????????????
        for (let i = 1; i <= topNum; i++) {
          //??????????????????
          const indexNum = topPosition - i;
          this.showTableList.unshift(this.tableList[indexNum]);
        }
        this.loadedNum = this.loadedNum + topNum - bottomNum; //????????????????????????????????????
        this.dataTop = this.dataTop - topNum * this.tdHeight; //?????????????????????????????????
        document.getElementById("bottomDiv").scrollTop =
          document.getElementById("bottomDiv").scrollTop +
          bottomNum * this.tdHeight;
        // this.scrollTop = document.getElementById("bottomDiv").scrollTop;
      } else if (type == "bottom") {
        this.showTableList.splice(0, topNum); //??????????????????
        for (let i = 0; i < bottomNum; i++) {
          //??????????????????
          const indexNum = topPosition + this.loadedNum + i;
          this.showTableList.push(this.tableList[indexNum]);
        }
        this.loadedNum = this.loadedNum - topNum + bottomNum; //????????????????????????????????????
        this.dataTop = this.dataTop + topNum * this.tdHeight; //?????????????????????????????????
        document.getElementById("bottomDiv").scrollTop =
          document.getElementById("bottomDiv").scrollTop -
          topNum * this.tdHeight;
        // this.scrollTop = document.getElementById("bottomDiv").scrollTop;
      } else if (type == "bottomAll") {
        this.showTableList = []; //??????????????????
        let scrollNum = topNum;
        for (let i = 0; i < bottomNum; i++) {
          //??????????????????
          let indexNum = scrollNum - this.loadNum + i;
          this.showTableList.push(this.tableList[indexNum]);
        }
        this.loadedNum = bottomNum; //????????????????????????????????????
        this.dataTop = (scrollNum - this.loadNum) * this.tdHeight; //?????????????????????????????????
        // this.scrollTop = document.getElementById("bottomDiv").scrollTop;
      } else if (type == "topAll") {
        this.showTableList = []; //??????????????????
        let scrollNum = bottomNum;
        for (let i = 0; i < topNum; i++) {
          //??????????????????
          let indexNum = scrollNum - topNum + this.loadNum + i;
          this.showTableList.push(this.tableList[indexNum]);
        }
        this.loadedNum = topNum; //????????????????????????????????????
        this.dataTop = (scrollNum - topNum + this.loadNum) * this.tdHeight; //?????????????????????????????????
        // this.scrollTop = document.getElementById("bottomDiv").scrollTop;
      }
      this.showLoad = false;
    },
    rowClick(item, index) {
      if (this.highlightRow !== undefined) {
        this.selectIndex = index;
      }
      this.$emit("on-current-change", item, index);
    },
    rowDblclick(index, item) {
      this.$emit("on-row-dblclick", item, index);
    },
    headDblclick(e) {
      if (e && e.target && e.target.dataset.colIndex) {
        let index = e.target.dataset.colIndex
        this.$emit("on-head-dblclick", this.columns[index], index);
        const selection = window.getSelection();
        selection.selectAllChildren(e.target);
      } else {
        window.getSelection().removeAllRanges();
      }
    },
    //??????
    handleSort(index, type) {
      let column = this.columns[index];
      let key = column.key;
      if (this.sortType.type === type && this.sortType.index === index) {
        type = 'normal';
      }
      this.sortType = {
        index,
        type
      };
      if (this.outerSort !== undefined) {
        this.$emit("on-sort-change", {
          column,
          key,
          order: type
        });
        return
      }

      this.tableList.sort((a, b) => {
        if (column.sortMethod) {
          return column.sortMethod(a[key], b[key], type);
        } else {
          if (type === "asc") {
            return a[key] > b[key] ? 1 : -1;
          } else if (type === "desc") {
            return a[key] < b[key] ? 1 : -1;
          }
        }
      });
    },
    initTable() {
      document.getElementById("bottomDiv") &&
        (document.getElementById("bottomDiv").scrollTop = 0);
      // ????????????????????????tab??????????????????????????????????????????????????????
      // document.getElementById("bottomDiv") &&
      //   (document.getElementById("bottomDiv").scrollLeft = 0);
      this.loadedNum = 0; //???????????????????????????
      this.dataTotal = 0; //???????????????
      this.dataTop = 0; //???????????????????????????
      this.scrollTop = 0; //?????????????????????
      this.showTableList = [];
      if (this.tableList.length > 0) {
        this.dataTotal = this.tableList.length; //??????????????????
        if (this.dataTotal >= this.loadNum) {
          //?????????????????????????????????????????????????????????
          this.loadedNum = this.loadNum; //????????????????????????
          for (var i = 0; i < this.loadNum; i++) {
            let data = this.tableList[i];
            this.showTableList.push(data);
          }
        } else if (this.dataTotal < this.loadNum) {
          this.loadedNum = this.dataTotal;
          for (let i = 0; i < this.dataTotal; i++) {
            let data = this.tableList[i];
            this.showTableList.push(data);
          }
        }
      }
      if(this.showTableList.length < 1) {
        this.$nextTick(()=>{
          this.getNoDataStyle()
        })
      }

    },
    getNoDataStyle() {
      let topTable = document.getElementById("topTable");
      if (topTable) {
        this.noDataStyle.width =  topTable.clientWidth + 'px'
      }
    },
    // ?????????????????????????????????
    scrollBarWidthCalc() {
      let bottomDiv = document.getElementById("bottomDiv");
      return  bottomDiv && bottomDiv.scrollHeight > bottomDiv.clientHeight ? this.scrollBarWidth : 0
    },
  },
  created() {
    this.handleScroll = this.proxy(this.scrollProcessing, 240, {
      leading: true,
      context: this
    });
  },
  mounted() {
    this.initTable();
  },
  watch: {
    tableList: {
      handler(newValue, oldValue) {
        this.initTable();
        if (oldValue) {
          this.scrollProcessing();
        }
      }
    },
    tableHeight(newValue) {
      if (newValue) {
        this.scrollProcessing(); //??????????????????????????????
      }
    }
  }
};
</script>
<style  scoped>
@import "./table.css";
</style>
