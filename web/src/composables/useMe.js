import { ref } from 'vue'
import { userApi } from '@/api/org'

/**
 * 当前登录用户名 / 头像首字（按需拉取，失败静默）。
 * 供工作台、管理后台等只需「名字/首字」的场景共用，避免各处重复实现 me() 拉取。
 * 需要头像 URL 等更多信息的场景（如主布局顶栏）自行处理。
 */
export function useMe(fallbackInitial = 'M') {
  const name = ref('')
  const initial = ref(fallbackInitial)
  userApi.me()
    .then((me) => {
      name.value = me.name || me.username || ''
      initial.value = (name.value || fallbackInitial).charAt(0)
    })
    .catch(() => { /* 取不到用户信息保持默认 */ })
  return { name, initial }
}
