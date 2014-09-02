wonum in (Select top 10 wonum from workorder 
Where 
 Status in ('DISPATCH','TRAVEL','ONSITE','START','ONHOLD','WOCOMP')
And woclass='WORKORDER' and worktype='PPM'
And wonum in (Select wonum from assignment where laborcode in (Select laborcode from labor where    IRV_CONTMETHOD='MOBILE' and personid=:personid ) and status = 'ASSIGNED'))