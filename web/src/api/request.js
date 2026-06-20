import axios from 'axios'
import { ElMessage } from 'element-plus'
import { TOKEN_KEY } from '@/store/user'
import { OPS_TOKEN_KEY } from '@/store/opsUser'

/**
 * Axios 封装（对齐 docs/api-conventions.md）：
 * - 请求拦截：注入 JWT（Authorization: Bearer）。
 * - 响应拦截：拆统一包装 R<T>，code=0 返回 data，否则错误码 toast 并 reject。
 * - 网络/HTTP 异常：toast 提示。
 * baseURL=/api/v1，开发期由 Vite 代理到后端。
 */
const request = axios.create({
  baseURL: '/api/v1',
  timeout: 15000,
})

request.interceptors.request.use((config) => {
  // 同一实例承载两套登录态：/ops 路径用运营 token，其余用租户 token，
  // 避免运营后台与租户应用相互覆盖鉴权。
  const isOps = location.pathname.startsWith('/ops')
  const token = localStorage.getItem(isOps ? OPS_TOKEN_KEY : TOKEN_KEY)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  (response) => {
    const body = response.data
    // 非统一包装（如文件流）直接返回
    if (body == null || typeof body.code === 'undefined') {
      return body
    }
    if (body.code === 0) {
      return body.data
    }
    ElMessage.error(body.message || '请求失败')
    return Promise.reject(body)
  },
  (error) => {
    const status = error.response?.status
    const msg = status === 401
      ? '未认证或登录已过期'
      : (error.response?.data?.message || error.message || '网络异常')
    ElMessage.error(msg)
    return Promise.reject(error)
  },
)

export default request
