# Villager Announcer
 
This minecraft plugin will announce villager deaths, infections and more to all players on the server.
Just place the jar file into your plugins directory and you're good to good.

You can change the options in config.yml:
```yaml
enabled: true
play-sound: true
sound-name: 'ENTITY_VILLAGER_DEATH'

broadcast-worlds: ['*']
# only broadcasts if they were formally normal villagers
broadcast-zombie-villager-deaths: true
# villagerannouncer.receive-broadcasts
players-require-premissions: false

messages:
  # all four of these settings will populate the %villager% variable:
  villager: 'villager'
  baby-villager: 'baby villager'
  zombie-villager: 'zombie villager'
  baby-zombie-villager: 'baby zombie villager'

  # populates %location% variable:
  location: '&r( &6XYZ: %x% %y% %z%, &r&ein &r&a%world-name%&r)'
  villager-message-with-profession: '&eA %villager% has died! Profession: %villager-profession%, level: %villager-level% %location%'
  villager-message: '&eA %villager% has died! %location%'
  villager-infection-with-profession: '&eA %villager% has been infected! Profession: %villager-profession%, level: %villager-level% %location%'
  villager-infection: '&eA %villager% has been infected! %location%'
  death-by-entity: '&eA %villager% was killed by %entity% %location%'
  death-by-misc: '&eA %villager% died by %death-cause% %location%'

# Available variables:
# %villager%
# %player%
# %entity%
# %entity-type%
# %death-cause%
# %world-name%
# %world-type%
# %villager-profession%
# %villager-level%
# %villager-experience%
# %villager-type%

file-version: 1
```

Questions or feature suggestions? Join the [discord](https://discord.gg/arcaneplugins-752310043214479462) server.