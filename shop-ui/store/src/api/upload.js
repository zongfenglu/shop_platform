import http from './http'

/**
 * 图片上传与素材库。对应 StoreUploadController。
 *
 * 上传走 multipart/form-data —— 不要手工设置 Content-Type，
 * 让 axios 根据 FormData 自动带上带 boundary 的头，手写会导致后端解析不出文件。
 */
export function uploadImage(file, groupId) {
  const form = new FormData()
  form.append('file', file)
  if (groupId != null && groupId !== '') {
    form.append('groupId', groupId)
  }
  return http.post('/store/upload/image', form)
}

/** 仅支持 MP4（后端按 magic byte 校验），上限 50MB */
export function uploadVideo(file, groupId) {
  const form = new FormData()
  form.append('file', file)
  if (groupId != null && groupId !== '') {
    form.append('groupId', groupId)
  }
  return http.post('/store/upload/video', form)
}

export function pageMaterials(params) {
  return http.get('/store/materials', { params })
}

export function deleteMaterial(id) {
  return http.delete(`/store/materials/${id}`)
}

export function moveMaterial(id, groupId) {
  return http.put(`/store/materials/${id}/group`, { groupId: groupId ?? null })
}

export function listMaterialGroups() {
  return http.get('/store/material-groups')
}

export function createMaterialGroup(name) {
  return http.post('/store/material-groups', { name })
}

export function renameMaterialGroup(id, name) {
  return http.put(`/store/material-groups/${id}`, { name })
}

export function deleteMaterialGroup(id) {
  return http.delete(`/store/material-groups/${id}`)
}
