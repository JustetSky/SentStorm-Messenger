import keycloak from './keycloak'

export async function initKeycloak() {
  const authenticated = await keycloak.init({
    onLoad: 'login-required',
    checkLoginIframe: false
  })

  if (!authenticated) {
    window.location.reload()
  }

  return keycloak
}
