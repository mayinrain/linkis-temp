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
  <div
    class="login"
    @keyup.enter.stop.prevent="handleSubmit('loginForm')">
    <i class="login-bg"/>
    <div class="login-main">
      <Form
        ref="loginForm"
        :model="loginForm"
        :rules="ruleInline">
        <FormItem>
          <span class="login-title">{{$t('message.common.login.loginTitle')}}</span>
        </FormItem>
        <FormItem prop="user">
          <div class="label">{{$t('message.linkis.userName')}}</div>
          <Input
            v-model="loginForm.user"
            type="text"
            :placeholder="$t('message.common.login.userName')"
            size="large"/>
        </FormItem>
        <FormItem prop="password">
          <div class="label">{{$t('message.linkis.password')}}</div>
          <Input
            v-model="loginForm.password"
            type="password"
            :placeholder="$t('message.common.login.passwordHint')"
            size="large" />
          <Checkbox
            v-model="rememberUserNameAndPass"
            class="remember-user-name"
            style="">{{$t('message.common.login.remenber')}}</Checkbox>
        </FormItem>
        <FormItem>
          <Button
            :loading="loading"
            type="primary"
            long
            size="large"
            @click="handleSubmit('loginForm')">{{$t('message.common.login.login')}}</Button>
        </FormItem>
      </Form>
    </div>
  </div>
</template>
<script>
import api from '@/common/service/api';
import storage from '@/common/helper/storage';
import { db } from '@/common/service/db/index.js';
import { config } from '@/common/config/db.js';
import JSEncrypt from 'jsencrypt';
import tab from '@/apps/scriptis/service/db/tab.js';
export default {
  data() {
    return {
      loading: false,
      loginForm: {
        user: '',
        password: '',
      },
      ruleInline: {
        user: [
          { required: true, message: this.$t('message.common.login.userName'), trigger: 'blur' },
          // {type: 'string', pattern: /^[0-9a-zA-Z\.\-_]{4,16}$/, message: '?????????????????????', trigger: 'change'},
        ],
        password: [
          { required: true, message: this.$t('message.common.login.password'), trigger: 'blur' },
          // {type: 'string', pattern: /^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])[0-9a-zA-Z]{8,18}$/, message: '?????????6???12????????????', trigger: 'change'},
        ],
      },
      rememberUserNameAndPass: false,
      publicKeyData: null
    };
  },
  created() {
    let userNameAndPass = storage.get('saveUserNameAndPass', 'local');
    if (userNameAndPass) {
      this.rememberUserNameAndPass = true;
      this.loginForm.user = userNameAndPass.split('&')[0];
      this.loginForm.password = userNameAndPass.split('&')[1];
    }
    this.getPublicKey();
  },
  mounted() {
  },
  methods: {
    // ??????????????????
    getPublicKey() {
      api.fetch('/user/publicKey', 'get').then((res) => {
        this.publicKeyData = res;
      })
    },
    handleSubmit(name) {
      this.$refs[name].validate(async (valid) => {
        if (valid) {
          this.loading = true;
          if (!this.rememberUserNameAndPass) {
            storage.remove('saveUserNameAndPass', 'local');
          }
          this.loginForm.user = this.loginForm.user.toLocaleLowerCase();
          // ???????????????????????????????????????
          let password = this.loginForm.password;
           let params = {};
           if (this.publicKeyData && this.publicKeyData.enableLoginEncrypt) {
             const key = `-----BEGIN PUBLIC KEY-----${this.publicKeyData.publicKey}-----END PUBLIC KEY-----`;
             const encryptor = new JSEncrypt()
             encryptor.setPublicKey(key)
             password = encryptor.encrypt(this.loginForm.password);
             params = {
               userName: this.loginForm.user,
               password
             };
           } else {
             params = {
               userName: this.loginForm.user,
               password
             };
           }
          // ????????????????????????
          // ????????????????????????????????????????????????????????????????????????????????????????????????
          let tabs = await tab.get() || [];
          const tablist = storage.get(this.loginForm.user + 'tabs', 'local');
          if (!tablist || tablist.length <= 0) {
            storage.set(this.loginForm.user + 'tabs', tabs, 'local');
          }
          Object.keys(config.stores).map((key) => {
            db.db[key].clear();
          })
          api
            .fetch(`/user/login`, params)
            .then((rst) => {
              this.loading = false;
              storage.set('userName',rst.userName,'session')
              storage.set('enableWatermark',rst.enableWatermark ? true : false,'session')
              // ???????????????
              if (this.rememberUserNameAndPass) {
                storage.set('saveUserNameAndPass', `${this.loginForm.user}&${this.loginForm.password}`, 'local');
              }
              if (rst) {
                this.userName = rst.userName;
                this.$router.push({path: '/console'});
                this.$Message.success(this.$t('message.common.login.loginSuccess'));
              }
            })
            .catch((err) => {
              if (this.rememberUserNameAndPass) {
                storage.set('saveUserNameAndPass', `${this.loginForm.user}&${this.loginForm.password}`, 'local');
              }
              if (err.message.indexOf('??????????????????????????????????????????') !== -1) {
                this.getPageHomeUrl().then(() => {
                  this.$router.push({path: '/'});
                })
              }
              this.loading = false;
            });
        } else {
          this.$Message.error(this.$t('message.common.login.vaildFaild'));
        }
      });
    },
    // ??????????????????
    clearSession() {
      storage.clear();
    },
  },
};
</script>
<style lang="scss" src="@/dss/assets/styles/login.scss"></style>
